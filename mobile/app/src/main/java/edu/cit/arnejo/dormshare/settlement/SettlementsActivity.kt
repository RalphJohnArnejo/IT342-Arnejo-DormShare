package edu.cit.arnejo.dormshare.settlement

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import kotlinx.coroutines.launch

/**
 * Settlements screen — matches web Settlement.jsx.
 * Uses correct endpoints: /api/ledger/summary and /api/ledger/history.
 * Features: You Owe / You Receive / History tabs, group selector,
 *           Stripe card payment + manual settle.
 */
class SettlementsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var rvSettlements: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvHeader: TextView
    private lateinit var spinnerGroup: Spinner

    private var groups: List<Group> = emptyList()
    private var selectedGroupId: Long? = null

    private var allDebts: List<SettlementDebt> = emptyList()
    private var historyItems: List<SettlementHistoryItem> = emptyList()

    private lateinit var debtAdapter: SettlementDebtAdapter
    private lateinit var historyAdapter: SettlementHistoryAdapter

    // Track selected payment method in dialog
    private var useStripe = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settlements)

        initViews()
        setupTabs()

        selectedGroupId = intent.getLongExtra("groupId", -1L).takeIf { it != -1L }
            ?: SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        loadGroups()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        rvSettlements = findViewById(R.id.rvSettlements)
        swipeRefresh = findViewById(R.id.swipeRefreshSettlements)
        tvEmpty = findViewById(R.id.tvEmptySettlements)
        tvHeader = findViewById(R.id.tvSettlementHeader)
        spinnerGroup = findViewById(R.id.spinnerGroup)

        rvSettlements.layoutManager = LinearLayoutManager(this)

        debtAdapter = SettlementDebtAdapter(emptyList()) { debt ->
            showPaymentDialog(debt)
        }
        historyAdapter = SettlementHistoryAdapter(emptyList())

        rvSettlements.adapter = debtAdapter

        swipeRefresh.setOnRefreshListener { loadSettlementData() }
        swipeRefresh.setColorSchemeColors(0xFFC49A3C.toInt())
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("You Owe"))
        tabLayout.addTab(tabLayout.newTab().setText("You Receive"))
        tabLayout.addTab(tabLayout.newTab().setText("History"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { updateList() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups()
                if (response.isSuccessful) {
                    groups = response.body()?.data ?: emptyList()
                    if (groups.isNotEmpty()) {
                        setupGroupSpinner()
                        if (selectedGroupId == null) {
                            selectedGroupId = groups[0].id
                        }
                        loadSettlementData()
                    } else {
                        tvEmpty.text = "Join a group first to view settlements"
                        tvEmpty.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettlementsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGroupSpinner() {
        val names = groups.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGroup.adapter = adapter

        val idx = groups.indexOfFirst { it.id == selectedGroupId }
        if (idx >= 0) spinnerGroup.setSelection(idx)

        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val newGroupId = groups[pos].id
                if (newGroupId != selectedGroupId) {
                    selectedGroupId = newGroupId
                    loadSettlementData()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerGroup.visibility = if (groups.size > 1) View.VISIBLE else View.GONE
    }

    private fun loadSettlementData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val summaryResponse = RetrofitClient.apiService.getLedgerSummary(selectedGroupId)
                if (summaryResponse.isSuccessful) {
                    allDebts = summaryResponse.body()?.data?.debts ?: emptyList()
                }

                val historyResponse = RetrofitClient.apiService.getLedgerHistory(selectedGroupId)
                if (historyResponse.isSuccessful) {
                    historyItems = historyResponse.body()?.data ?: emptyList()
                }

                updateList()
            } catch (e: Exception) {
                Toast.makeText(this@SettlementsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList() {
        val currentUserId = SessionManager.getUserId(this)
        val tab = tabLayout.selectedTabPosition

        when (tab) {
            0 -> { // You Owe
                val pending = allDebts.filter {
                    it.status == "PENDING" && it.fromUserId == currentUserId
                }
                debtAdapter.update(pending, showPayButton = true)
                rvSettlements.adapter = debtAdapter
                tvEmpty.text = if (pending.isEmpty()) "No pending payments. You're all set!" else ""
                tvEmpty.visibility = if (pending.isEmpty()) View.VISIBLE else View.GONE
                tvHeader.text = "Your Pending Payments"
            }
            1 -> { // You Receive
                val receiving = allDebts.filter {
                    it.status == "PENDING" && it.toUserId == currentUserId
                }
                debtAdapter.update(receiving, showPayButton = false)
                rvSettlements.adapter = debtAdapter
                tvEmpty.text = if (receiving.isEmpty()) "No pending payments from others" else ""
                tvEmpty.visibility = if (receiving.isEmpty()) View.VISIBLE else View.GONE
                tvHeader.text = "Payments You're Receiving"
            }
            2 -> { // History
                historyAdapter.update(historyItems)
                rvSettlements.adapter = historyAdapter
                tvEmpty.text = if (historyItems.isEmpty()) "No settlement history yet" else ""
                tvEmpty.visibility = if (historyItems.isEmpty()) View.VISIBLE else View.GONE
                tvHeader.text = "Settlement History"
            }
        }
    }

    /**
     * Shows the Payment Details dialog matching the web Settlement.jsx payment modal.
     * Two options: Card Payment (Stripe) or Mark as Settled (Manual).
     */
    private fun showPaymentDialog(debt: SettlementDebt) {
        useStripe = true
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_details, null)

        // Populate payee info
        dialogView.findViewById<TextView>(R.id.tvPayeeName).text = debt.toUserName ?: "User"
        dialogView.findViewById<TextView>(R.id.tvPaymentAmount).text =
            "\u20B1${"%.2f".format(debt.amount)}"

        val radioStripe = dialogView.findViewById<View>(R.id.radioStripe)
        val radioManual = dialogView.findViewById<View>(R.id.radioManual)
        val optionStripe = dialogView.findViewById<View>(R.id.optionStripe)
        val optionManual = dialogView.findViewById<View>(R.id.optionManual)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelPayment)

        fun updateRadios() {
            radioStripe.setBackgroundResource(
                if (useStripe) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
            )
            radioManual.setBackgroundResource(
                if (!useStripe) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
            )
            btnConfirm.text = if (useStripe) "Confirm Payment" else "Mark as Settled"
        }

        optionStripe.setOnClickListener { useStripe = true; updateRadios() }
        optionManual.setOnClickListener { useStripe = false; updateRadios() }
        updateRadios()

        val dialog = AlertDialog.Builder(this, R.style.Theme_DormShare_Dialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            btnConfirm.isEnabled = false
            btnConfirm.text = "Processing..."

            if (useStripe) {
                processStripePayment(debt, dialog, btnConfirm)
            } else {
                processManualSettle(debt, dialog, btnConfirm)
            }
        }

        dialog.show()
    }

    /**
     * Stripe payment flow:
     * 1. POST /api/payments/stripe/intent  → get paymentIntentId
     * 2. POST /api/payments/stripe/confirm → auto-succeed in mock/sandbox mode
     * 3. Backend marks the split as settled
     */
    private fun processStripePayment(
        debt: SettlementDebt, dialog: AlertDialog, btn: MaterialButton
    ) {
        lifecycleScope.launch {
            try {
                // Step 1: Create payment intent
                val intentBody = mapOf<String, Any>(
                    "payeeId" to (debt.toUserId),
                    "amount" to debt.amount,
                    "groupId" to (selectedGroupId ?: 0L),
                    "description" to "Settlement to ${debt.toUserName}"
                )
                val intentResponse = RetrofitClient.apiService.createStripeIntent(intentBody)

                if (!intentResponse.isSuccessful || intentResponse.body()?.success != true) {
                    val msg = intentResponse.body()?.error?.message ?: "Failed to create payment"
                    showToast(msg)
                    resetBtn(btn, "Confirm Payment")
                    return@launch
                }

                @Suppress("UNCHECKED_CAST")
                val intentData = intentResponse.body()?.data as? Map<String, Any> ?: emptyMap()
                val paymentIntentId = intentData["paymentIntentId"]?.toString() ?: ""

                // Step 2: Confirm payment (sandbox auto-succeeds)
                val confirmBody = mapOf<String, Any>(
                    "paymentIntentId" to paymentIntentId,
                    "settlementId" to (debt.splitId?.toString() ?: "")
                )
                val confirmResponse = RetrofitClient.apiService.confirmStripePayment(confirmBody)

                if (confirmResponse.isSuccessful && confirmResponse.body()?.success == true) {
                    @Suppress("UNCHECKED_CAST")
                    val resultData = confirmResponse.body()?.data as? Map<String, Any> ?: emptyMap()
                    val status = resultData["status"]?.toString()
                    if (status == "succeeded") {
                        showToast("\uD83D\uDCB3 Payment successful! Settlement recorded.")
                        dialog.dismiss()
                        loadSettlementData()
                    } else {
                        showToast("Payment status: $status")
                        resetBtn(btn, "Confirm Payment")
                    }
                } else {
                    val msg = confirmResponse.body()?.error?.message ?: "Confirmation failed"
                    showToast(msg)
                    resetBtn(btn, "Confirm Payment")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                resetBtn(btn, "Confirm Payment")
            }
        }
    }

    /**
     * Manual settle — marks the split as settled without Stripe processing.
     */
    private fun processManualSettle(
        debt: SettlementDebt, dialog: AlertDialog, btn: MaterialButton
    ) {
        val splitId = debt.splitId ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.settleSplit(splitId)
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("\u2705 Marked as settled!")
                    dialog.dismiss()
                    loadSettlementData()
                } else {
                    val msg = response.body()?.error?.message ?: "Failed to settle"
                    showToast(msg)
                    resetBtn(btn, "Mark as Settled")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                resetBtn(btn, "Mark as Settled")
            }
        }
    }

    private fun resetBtn(btn: MaterialButton, text: String) {
        btn.isEnabled = true
        btn.text = text
    }

    private fun showToast(msg: String) {
        Toast.makeText(this@SettlementsActivity, msg, Toast.LENGTH_SHORT).show()
    }
}