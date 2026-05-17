package edu.cit.arnejo.dormshare.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.expense.ExpensesActivity
import edu.cit.arnejo.dormshare.group.GroupsActivity
import edu.cit.arnejo.dormshare.notification.NotificationsActivity
import edu.cit.arnejo.dormshare.pantry.PantryActivity
import edu.cit.arnejo.dormshare.profile.ProfileActivity
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.shared.auth.TokenProvider
import kotlinx.coroutines.launch

/**
 * Main dashboard — mirrors web Dashboard.jsx.
 * Shows expense summary, recent expense, recent updates (notifications),
 * and provides bottom navigation to all feature screens.
 */
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

    // Pantry overview
    private lateinit var tvPantryInStock: TextView
    private lateinit var tvPantryLowStock: TextView
    private lateinit var tvPantryOutOfStock: TextView
    private lateinit var tvPantryTotalItems: TextView

    // Hidden fields for backward compatibility
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView

    // Notification polling
    private val notificationHandler = Handler(Looper.getMainLooper())
    private val notificationRunnable = object : Runnable {
        override fun run() {
            loadUnreadCount()
            notificationHandler.postDelayed(this, 15_000) // Poll every 15s like web
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupUserGreeting()
        setupNavigation()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_dashboard
        // Start polling notifications
        notificationHandler.post(notificationRunnable)
    }

    override fun onPause() {
        super.onPause()
        notificationHandler.removeCallbacks(notificationRunnable)
    }

    private fun initViews() {
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

        // Pantry overview
        tvPantryInStock = findViewById(R.id.tvPantryInStock)
        tvPantryLowStock = findViewById(R.id.tvPantryLowStock)
        tvPantryOutOfStock = findViewById(R.id.tvPantryOutOfStock)
        tvPantryTotalItems = findViewById(R.id.tvPantryTotalItems)
    }

    private fun setupUserGreeting() {
        var firstName = intent.getStringExtra("firstName") ?: ""
        var lastName = intent.getStringExtra("lastName") ?: ""

        if (firstName.isEmpty() && lastName.isEmpty()) {
            firstName = SessionManager.getFirstName(this) ?: ""
            lastName = SessionManager.getLastName(this) ?: ""
        }

        val displayName = "$firstName $lastName".trim()
        tvWelcome.text = if (displayName.isNotEmpty()) "Welcome back, $displayName!" else "Welcome back!"
    }

    private fun setupNavigation() {
        // View all expenses link
        tvViewAllExpenses.setOnClickListener {
            navigateTo(ExpensesActivity::class.java)
        }

        // Pantry total items link opens pantry
        tvPantryTotalItems.setOnClickListener {
            navigateTo(PantryActivity::class.java)
        }

        // Recent updates section taps open notifications
        tvRecentUpdates.setOnClickListener {
            navigateTo(NotificationsActivity::class.java)
        }

        // Bottom Navigation — uses FLAG_ACTIVITY_REORDER_TO_FRONT to prevent stacking
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_groups -> {
                    navigateTo(GroupsActivity::class.java)
                    true
                }
                R.id.nav_pantry -> {
                    navigateTo(PantryActivity::class.java)
                    true
                }
                R.id.nav_expenses -> {
                    navigateTo(ExpensesActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Navigate to another activity, passing groupId and preventing infinite stacking.
     */
    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val gid = SessionManager.getSelectedGroupId(this)
        if (gid != -1L) intent.putExtra("groupId", gid)
        startActivity(intent)
    }

    private fun loadDashboardData() {
        val groupId = SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        lifecycleScope.launch {
            // Load recent expenses
            try {
                val expenseResponse = RetrofitClient.apiService.getExpenseLedger(groupId)
                if (expenseResponse.isSuccessful) {
                    val expenses = expenseResponse.body()?.data ?: emptyList()
                    if (expenses.isNotEmpty()) {
                        val recent = expenses.first()
                        tvRecentExpenseDesc.text = recent.description ?: "Expense"
                        tvRecentExpenseDate.text = buildString {
                            if (recent.payerName != null) append("Paid by ${recent.payerName}")
                            if (recent.date != null) {
                                if (isNotEmpty()) append(" • ")
                                append(recent.date)
                            }
                        }.ifEmpty { recent.date ?: "" }
                        tvRecentExpenseAmount.text = "₱%.2f".format(recent.amount)
                    } else {
                        tvRecentExpenseDesc.text = "No recent expenses"
                        tvRecentExpenseDate.text = ""
                        tvRecentExpenseAmount.text = ""
                    }
                }
            } catch (_: Exception) {
                // Dashboard will show defaults
            }

            // Load summary for balance cards
            try {
                val summaryResponse = RetrofitClient.apiService.getExpenseSummary(groupId)
                if (summaryResponse.isSuccessful) {
                    val summary = summaryResponse.body()?.data
                    if (summary != null) {
                        tvOwedToYou.text = "₱%.0f".format(summary.owedToYou)
                        tvYouOwe.text = "₱%.0f".format(summary.youOwe)
                        tvNetBalance.text = "₱%.0f".format(summary.netBalance)
                    }
                }
            } catch (_: Exception) { }

            // Load recent notification as update preview
            try {
                val notifResponse = RetrofitClient.apiService.getNotifications(limit = 1)
                if (notifResponse.isSuccessful) {
                    val notifications = notifResponse.body()?.data ?: emptyList()
                    if (notifications.isNotEmpty()) {
                        tvRecentUpdates.text = notifications.first().title ?: "New activity"
                    }
                }
            } catch (_: Exception) { }

            // Load pantry overview
            try {
                val pantryResponse = RetrofitClient.apiService.getPantryItems(groupId)
                if (pantryResponse.isSuccessful) {
                    val items = pantryResponse.body()?.data ?: emptyList()
                    val inStock = items.count { it.status == "IN" }
                    val lowStock = items.count { it.status == "LOW" }
                    val outOfStock = items.count { it.status == "OUT" }
                    tvPantryInStock.text = inStock.toString()
                    tvPantryLowStock.text = lowStock.toString()
                    tvPantryOutOfStock.text = outOfStock.toString()
                    tvPantryTotalItems.text = "${items.size} total items → →"
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadUnreadCount() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnreadNotificationCount()
                if (response.isSuccessful) {
                    val count = response.body()?.data ?: 0
                    // Show badge on profile tab (or create a dedicated notification icon)
                    val badge = bottomNav.getOrCreateBadge(R.id.nav_profile)
                    if (count > 0) {
                        badge.isVisible = true
                        badge.number = count
                    } else {
                        badge.isVisible = false
                    }
                }
            } catch (_: Exception) { }
        }
    }
}