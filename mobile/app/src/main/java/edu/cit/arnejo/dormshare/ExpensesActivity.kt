package edu.cit.arnejo.dormshare

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.adapter.ExpenseAdapter
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.auth.SessionManager
import edu.cit.arnejo.dormshare.databinding.ActivityExpensesBinding
import edu.cit.arnejo.dormshare.model.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private lateinit var adapter: ExpenseAdapter
    private var groupId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra("groupId", -1L).takeIf { it != -1L }
            ?: SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        adapter = ExpenseAdapter(
            emptyList(),
            onItemLongClick = { expense ->
                showExpenseOptions(expense)
                true
            }
        )
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter

        binding.fabAddExpense.setOnClickListener { showAddExpenseDialog() }

        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExpenses(groupId)
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    adapter.update(list)
                    binding.tvEmptyExpenses.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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
                    val list = response.body() ?: emptyList()
                    val total = list.sumOf { it.amount }
                    binding.tvOwedToYou.text = "₱%.0f".format(total)
                    binding.tvYouOwe.text = "₱0"
                    binding.tvNetBalance.text = "%.0f".format(total)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showAddExpenseDialog() {
        val input = TextInputEditText(this)
        input.hint = "Description"
        val amountInput = TextInputEditText(this)
        amountInput.hint = "Amount"

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(input)
            addView(amountInput)
        }
        container.setPadding(40, 0, 40, 40)

        AlertDialog.Builder(this)
            .setTitle("Log Expense")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val desc = input.text.toString().trim()
                val amount = amountInput.text.toString().trim().toDoubleOrNull()
                if (desc.isNotEmpty() && amount != null && amount > 0) {
                    addExpense(desc, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addExpense(description: String, amount: Double) {
        val userId = SessionManager.getUserId(this)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val expense = Expense(
            id = 0,
            description = description,
            amount = amount,
            groupId = groupId,
            date = date,
            paidById = userId
        )
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createExpense(expense, groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ExpensesActivity, "Expense added!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    Toast.makeText(this@ExpensesActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
        val input = TextInputEditText(this).apply { setText(expense.description) }
        val amountInput = TextInputEditText(this).apply { setText(expense.amount.toString()) }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(input)
            addView(amountInput)
        }
        container.setPadding(40, 0, 40, 40)

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
        val expense = Expense(id, description, amount, groupId, null, SessionManager.getUserId(this))
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateExpense(id, expense)
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
                deleteExpense(expense.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(id: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteExpense(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@ExpensesActivity, "Deleted!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    Toast.makeText(this@ExpensesActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpensesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
