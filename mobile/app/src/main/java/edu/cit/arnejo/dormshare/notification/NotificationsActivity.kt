package edu.cit.arnejo.dormshare.notification

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Notifications screen — mirrors the web notification panel.
 * Lists notifications, marks individual/all as read, supports pull-to-refresh.
 */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvUnreadCount: TextView
    private lateinit var btnMarkAllRead: MaterialButton
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        swipeRefresh = findViewById(R.id.swipeRefreshNotifications)
        tvEmpty = findViewById(R.id.tvEmptyNotifications)
        tvUnreadCount = findViewById(R.id.tvUnreadCount)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)

        adapter = NotificationAdapter(emptyList()) { notification ->
            if (!notification.isRead) {
                markRead(notification.id)
            }
        }

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadNotifications() }
        swipeRefresh.setColorSchemeColors(0xFFC49A3C.toInt())

        btnMarkAllRead.setOnClickListener { markAllRead() }

        loadNotifications()
    }

    private fun loadNotifications() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getNotifications(limit = 50)
                if (response.isSuccessful && response.body()?.success == true) {
                    val notifications = response.body()?.data ?: emptyList()
                    adapter.update(notifications)
                    tvEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

                    val unread = notifications.count { !it.isRead }
                    tvUnreadCount.text = if (unread > 0) "$unread unread" else "All caught up!"
                    btnMarkAllRead.visibility = if (unread > 0) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun markRead(id: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.markNotificationRead(id)
                if (response.isSuccessful) {
                    loadNotifications() // Refresh list
                }
            } catch (_: Exception) { }
        }
    }

    private fun markAllRead() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.markAllNotificationsRead()
                if (response.isSuccessful) {
                    Toast.makeText(this@NotificationsActivity, "All marked as read", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
