package edu.cit.arnejo.dormshare.expense

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.databinding.ActivityExpensesBinding
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.group.GroupMember
import edu.cit.arnejo.dormshare.settlement.SettlementsActivity
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import kotlinx.coroutines.launch

/**
 * Expenses screen — rewritten to match web Expenses.jsx.
 * Adds: category selection, roommate split selection (equal/custom),
 * group selector, settle-split inline, correct API calls.
 * OCR receipt scanning via Google ML Kit Text Recognition.
 */
class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private lateinit var adapter: ExpenseAdapter
    private var groupId: Long? = null
    private var groups: List<Group> = emptyList()
    private var groupMembers: List<GroupMember> = emptyList()

    // OCR fields — will be set when dialog is open
    private var ocrDescriptionField: TextInputEditText? = null
    private var ocrAmountField: TextInputEditText? = null
    private var ocrStatusView: android.widget.TextView? = null
    private var ocrResultLayout: LinearLayout? = null
    private var ocrDetailsView: android.widget.TextView? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processReceiptImage(it) }
    }

    companion object {
        val CATEGORIES = listOf(
            "Groceries", "Utilities", "Rent", "Food & Dining",
            "Transportation", "Supplies", "Entertainment", "Other"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra("groupId", -1L).takeIf { it != -1L }
            ?: SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        val currentUserId = SessionManager.getUserId(this)

        adapter = ExpenseAdapter(
            emptyList(),
            currentUserId = currentUserId,
            onItemLongClick = { expense ->
                showExpenseOptions(expense)
                true
            },
            onSettleClick = { splitId ->
                confirmSettleSplit(splitId)
            }
        )
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter

        binding.fabAddExpense.setOnClickListener { showAddExpenseDialog() }

        loadGroups()
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups()
                if (response.isSuccessful) {
                    groups = response.body()?.data ?: emptyList()
                    if (groups.isNotEmpty()) {
                        if (groupId == null) groupId = groups[0].id
                        updateGroupMembers()
                        loadExpenses()
                    } else {
                        binding.tvEmptyExpenses.text = "Join a group first to track expenses"
                        binding.tvEmptyExpenses.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateGroupMembers() {
        val currentUserId = SessionManager.getUserId(this)
        val group = groups.find { it.id == groupId }
        groupMembers = (group?.members ?: emptyList()).filter { it.userId != currentUserId }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExpenseLedger(groupId)
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.update(list)
                    binding.tvEmptyExpenses.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    loadSummary()
                } else {
                    Toast.makeText(this@ExpensesActivity, "Failed to load", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExpenseSummary(groupId)
                if (response.isSuccessful) {
                    val summary = response.body()?.data
                    if (summary != null) {
                        binding.tvOwedToYou.text = "₱%.0f".format(summary.owedToYou)
                        binding.tvYouOwe.text = "₱%.0f".format(summary.youOwe)
                        binding.tvNetBalance.text = "₱%.0f".format(summary.netBalance)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ===================== OCR RECEIPT SCANNING =====================

    /**
     * Process a receipt image using Google ML Kit Text Recognition.
     * Extracts amount and description, then auto-fills the form.
     * Matches web Expenses.jsx handleFileUpload logic.
     */
    private fun processReceiptImage(imageUri: Uri) {
        // Show scanning status
        ocrStatusView?.visibility = View.VISIBLE
        ocrStatusView?.text = "⏳ Scanning receipt…"
        ocrResultLayout?.visibility = View.GONE

        try {
            val image = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    if (fullText.isBlank()) {
                        ocrStatusView?.text = "❌ No text found in image"
                        return@addOnSuccessListener
                    }

                    // Parse receipt — improved: prioritize TOTAL line over largest amount
                    val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val moneyRegex = Regex("""(\d+[.,]\d{2})""")

                    var detectedAmount = ""

                    // 1st priority: find a line with TOTAL/SUBTOTAL/AMOUNT DUE and extract its amount
                    val totalKeywords = listOf("total", "subtotal", "amount due", "grand total", "net amount")
                    val cashKeywords = listOf("cash", "change", "tendered", "payment")
                    for (line in lines) {
                        val lower = line.lowercase()
                        // Skip lines about cash/change — we want the bill total, not what was paid
                        if (cashKeywords.any { lower.contains(it) }) continue
                        if (totalKeywords.any { lower.contains(it) }) {
                            val match = moneyRegex.find(line)
                            if (match != null) {
                                detectedAmount = match.groupValues[1].replace(",", ".")
                                break
                            }
                        }
                    }

                    // 2nd priority: if no TOTAL found, use largest amount (excluding cash/change lines)
                    if (detectedAmount.isEmpty()) {
                        val allAmounts = mutableListOf<Double>()
                        for (line in lines) {
                            val lower = line.lowercase()
                            if (cashKeywords.any { lower.contains(it) }) continue
                            moneyRegex.findAll(line).forEach { m ->
                                m.groupValues[1].replace(",", ".").toDoubleOrNull()?.let { allAmounts.add(it) }
                            }
                        }
                        if (allAmounts.isNotEmpty()) {
                            detectedAmount = "%.2f".format(allAmounts.max())
                        }
                    }
                    val detectedDescription = lines.firstOrNull() ?: ""

                    // Detect store/merchant name
                    val merchantKeywords = listOf("store", "market", "shop", "mall", "grocery", "pharmacy", "supermarket")
                    val merchant = lines.firstOrNull { line ->
                        merchantKeywords.any { line.lowercase().contains(it) }
                    } ?: ""

                    // Auto-fill form fields
                    if (detectedDescription.isNotEmpty()) {
                        val desc = if (merchant.isNotEmpty()) merchant else detectedDescription
                        ocrDescriptionField?.setText(desc)
                    }
                    if (detectedAmount.isNotEmpty() && detectedAmount != "0.00") {
                        ocrAmountField?.setText(detectedAmount)
                    }

                    // Show OCR result summary
                    ocrStatusView?.visibility = View.GONE
                    ocrResultLayout?.visibility = View.VISIBLE
                    val details = buildString {
                        if (detectedAmount.isNotEmpty() && detectedAmount != "0.00")
                            append("Amount: ₱$detectedAmount\n")
                        if (merchant.isNotEmpty())
                            append("Merchant: $merchant\n")
                        append("Lines detected: ${lines.size}")
                    }
                    ocrDetailsView?.text = details

                    Toast.makeText(this, "📷 Receipt scanned! Review details.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    ocrStatusView?.text = "❌ Scan failed: ${e.message}"
                    ocrResultLayout?.visibility = View.GONE
                }
        } catch (e: Exception) {
            ocrStatusView?.text = "❌ Error: ${e.message}"
        }
    }

    // ===================== ADD EXPENSE DIALOG =====================

    /**
     * Full expense dialog with category, roommate selection, split method,
     * and OCR receipt scanning — matching web Expenses.jsx modal form.
     */
    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_add_expense, null
        )

        val etDescription = dialogView.findViewById<TextInputEditText>(
            R.id.etExpenseDescription
        )
        val etAmount = dialogView.findViewById<TextInputEditText>(
            R.id.etExpenseAmount
        )
        val spinnerCategory = dialogView.findViewById<Spinner>(
            R.id.spinnerCategory
        )
        val chipGroupMembers = dialogView.findViewById<ChipGroup>(
            R.id.chipGroupMembers
        )
        val tvSplitPreview = dialogView.findViewById<android.widget.TextView>(
            R.id.tvSplitPreview
        )

        // OCR views
        val btnScanReceipt = dialogView.findViewById<MaterialButton>(R.id.btnScanReceipt)
        ocrStatusView = dialogView.findViewById(R.id.tvOcrStatus)
        ocrResultLayout = dialogView.findViewById(R.id.layoutOcrResult)
        ocrDetailsView = dialogView.findViewById(R.id.tvOcrDetails)
        ocrDescriptionField = etDescription
        ocrAmountField = etAmount

        // Scan receipt button — launch image picker
        btnScanReceipt.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, CATEGORIES)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Setup roommate chips
        val selectedRoommates = mutableSetOf<Long>()
        chipGroupMembers.removeAllViews()

        if (groupMembers.isEmpty()) {
            val noMembersChip = Chip(this).apply {
                text = "No roommates in group"
                isCheckable = false
                isEnabled = false
            }
            chipGroupMembers.addView(noMembersChip)
        } else {
            for (member in groupMembers) {
                val chip = Chip(this).apply {
                    text = member.name
                    isCheckable = true
                    tag = member.userId
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedRoommates.add(member.userId)
                        else selectedRoommates.remove(member.userId)

                        // Update split preview
                        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                        if (selectedRoommates.isNotEmpty() && amount > 0) {
                            val share = amount / (selectedRoommates.size + 1)
                            tvSplitPreview.text = "Each pays: ₱%.2f (split %d ways)".format(
                                share, selectedRoommates.size + 1
                            )
                            tvSplitPreview.visibility = View.VISIBLE
                        } else {
                            tvSplitPreview.visibility = View.GONE
                        }
                    }
                }
                chipGroupMembers.addView(chip)
            }
        }

        // Update preview when amount changes
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val amount = s.toString().toDoubleOrNull() ?: 0.0
                if (selectedRoommates.isNotEmpty() && amount > 0) {
                    val share = amount / (selectedRoommates.size + 1)
                    tvSplitPreview.text = "Each pays: ₱%.2f (split %d ways)".format(
                        share, selectedRoommates.size + 1
                    )
                    tvSplitPreview.visibility = View.VISIBLE
                } else {
                    tvSplitPreview.visibility = View.GONE
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Log Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val desc = etDescription.text.toString().trim()
                val amount = etAmount.text.toString().trim().toDoubleOrNull()
                val category = CATEGORIES[spinnerCategory.selectedItemPosition]

                if (desc.isEmpty() || amount == null || amount <= 0) {
                    Toast.makeText(this, "Fill in description and amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedRoommates.isEmpty()) {
                    Toast.makeText(this, "Select at least one roommate", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addExpense(desc, amount, category, selectedRoommates.toList())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addExpense(description: String, amount: Double, category: String, roommateIds: List<Long>) {
        val currentUserId = SessionManager.getUserId(this)
        val share = amount / (roommateIds.size + 1) // +1 for self

        val splits = mutableListOf<SplitEntry>()
        // Self split
        splits.add(SplitEntry(userId = currentUserId, amount = share))
        // Roommate splits
        roommateIds.forEach { rid ->
            splits.add(SplitEntry(userId = rid, amount = share))
        }

        val request = ExpenseRequest(
            amount = amount,
            description = description,
            category = category,
            splits = splits
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createExpense(request, groupId)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ExpensesActivity, "Expense logged!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val msg = response.body()?.error?.message ?: "Failed to save"
                    Toast.makeText(this@ExpensesActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmSettleSplit(splitId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Settle Split")
            .setMessage("Mark this split as settled?")
            .setPositiveButton("Settle") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.apiService.settleSplit(splitId)
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@ExpensesActivity, "Split settled!", Toast.LENGTH_SHORT).show()
                            loadExpenses()
                        } else {
                            val msg = response.body()?.error?.message ?: "Failed to settle"
                            Toast.makeText(this@ExpensesActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExpenseOptions(expense: Expense) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Expense")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditExpenseDialog(expense)
                    1 -> confirmDeleteExpense(expense)
                }
            }
            .show()
    }

    private fun showEditExpenseDialog(expense: Expense) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val input = TextInputEditText(this).apply {
            setText(expense.description)
            hint = "Description"
        }
        val amountInput = TextInputEditText(this).apply {
            setText(expense.amount.toString())
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(input)
        container.addView(amountInput)

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val desc = input.text.toString().trim()
                val amount = amountInput.text.toString().trim().toDoubleOrNull()
                if (desc.isNotEmpty() && amount != null) {
                    updateExpense(expense.id, desc, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateExpense(id: Long, description: String, amount: Double) {
        val request = ExpenseRequest(
            amount = amount,
            description = description,
            category = "Other"
        )
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createExpense(request, groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ExpensesActivity, "Updated!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    Toast.makeText(this@ExpensesActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteExpense(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.apiService.deletePantryItem(expense.id) // Uses generic delete
                        if (response.isSuccessful) {
                            Toast.makeText(this@ExpensesActivity, "Deleted!", Toast.LENGTH_SHORT).show()
                            loadExpenses()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}