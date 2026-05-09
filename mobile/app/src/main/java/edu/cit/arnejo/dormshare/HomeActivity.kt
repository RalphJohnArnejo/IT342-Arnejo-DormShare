package edu.cit.arnejo.dormshare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.auth.SessionManager
import edu.cit.arnejo.dormshare.auth.TokenProvider
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvOwedToYou: TextView
    private lateinit var tvYouOwe: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var tvRecentExpenseDesc: TextView
    private lateinit var tvRecentExpenseDate: TextView
    private lateinit var tvRecentExpenseAmount: TextView
    private lateinit var tvViewAllExpenses: TextView
    private lateinit var tvRecentUpdates: TextView
    private lateinit var bottomNav: BottomNavigationView

    // Hidden fields for backward compatibility
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvOwedToYou = findViewById(R.id.tvOwedToYou)
        tvYouOwe = findViewById(R.id.tvYouOwe)
        tvNetBalance = findViewById(R.id.tvNetBalance)
        tvRecentExpenseDesc = findViewById(R.id.tvRecentExpenseDesc)
        tvRecentExpenseDate = findViewById(R.id.tvRecentExpenseDate)
        tvRecentExpenseAmount = findViewById(R.id.tvRecentExpenseAmount)
        tvViewAllExpenses = findViewById(R.id.tvViewAllExpenses)
        tvRecentUpdates = findViewById(R.id.tvRecentUpdates)
        bottomNav = findViewById(R.id.bottomNav)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)

        // Get user data from intent or session
        var firstName = intent.getStringExtra("firstName") ?: ""
        var lastName = intent.getStringExtra("lastName") ?: ""

        // Fallback to session if intent extras are missing
        if (firstName.isEmpty() && lastName.isEmpty()) {
            firstName = SessionManager.getFirstName(this) ?: ""
            lastName = SessionManager.getLastName(this) ?: ""
        }

        // Display welcome message
        val displayName = "$firstName $lastName".trim()
        tvWelcome.text = if (displayName.isNotEmpty()) "Welcome back, $displayName!" else "Welcome back!"

        // View all expenses link
        tvViewAllExpenses.setOnClickListener {
            val intent = Intent(this, ExpensesActivity::class.java)
            val selectedGroupId = SessionManager.getSelectedGroupId(this)
            if (selectedGroupId != -1L) {
                intent.putExtra("groupId", selectedGroupId)
            }
            startActivity(intent)
        }

        // Bottom Navigation
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_groups -> {
                    startActivity(Intent(this, GroupsActivity::class.java))
                    true
                }
                R.id.nav_pantry -> {
                    val intent = Intent(this, PantryActivity::class.java)
                    val gid = SessionManager.getSelectedGroupId(this)
                    if (gid != -1L) intent.putExtra("groupId", gid)
                    startActivity(intent)
                    true
                }
                R.id.nav_expenses -> {
                    val intent = Intent(this, ExpensesActivity::class.java)
                    val gid = SessionManager.getSelectedGroupId(this)
                    if (gid != -1L) intent.putExtra("groupId", gid)
                    startActivity(intent)
                    true
                }
                R.id.nav_settlements -> {
                    val intent = Intent(this, SettlementsActivity::class.java)
                    val gid = SessionManager.getSelectedGroupId(this)
                    if (gid != -1L) intent.putExtra("groupId", gid)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Load dashboard data
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_dashboard
    }

    private fun loadDashboardData() {
        val groupId = SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        lifecycleScope.launch {
            try {
                // Load recent expenses
                val expenseResponse = RetrofitClient.apiService.getExpenses(groupId)
                if (expenseResponse.isSuccessful) {
                    // CHANGE: Use .data from the ApiResponse wrapper
                    val expenses = expenseResponse.body()?.data ?: emptyList()
                    if (expenses.isNotEmpty()) {
                        val recent = expenses.first()
                        tvRecentExpenseDesc.text = recent.description ?: "Expense"
                        tvRecentExpenseDate.text = recent.date ?: ""
                        tvRecentExpenseAmount.text = "₱%.2f".format(recent.amount)
                    } else {
                        tvRecentExpenseDesc.text = "No recent expenses"
                        tvRecentExpenseDate.text = ""
                        tvRecentExpenseAmount.text = ""
                    }
                }
            } catch (_: Exception) {
                // Silently handle - dashboard will show defaults
            }

            try {
                // Load summary for balance cards
                val summaryResponse = RetrofitClient.apiService.getExpenseSummary(groupId)
                if (summaryResponse.isSuccessful) {
                    val summary = summaryResponse.body()?.data
                    if (summary != null) {
                        tvOwedToYou.text = "₱%.0f".format(summary.owedToYou)
                        tvYouOwe.text = "₱%.0f".format(summary.youOwe)
                        tvNetBalance.text = "₱%.0f".format(summary.netBalance)
                    }
                }
            } catch (_: Exception) {
                // Keep defaults
            }
        }
    }

    private fun showProfileOptions() {
        val options = arrayOf("Logout")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> logout()
                }
            }
            .show()
    }

    private fun logout() {
        SessionManager.clearSession(this)
        TokenProvider.token = null
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}