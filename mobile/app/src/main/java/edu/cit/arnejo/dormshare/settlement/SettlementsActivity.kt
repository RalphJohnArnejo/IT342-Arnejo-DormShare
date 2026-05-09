package edu.cit.arnejo.dormshare.settlement

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.cit.arnejo.dormshare.settlement.SettlementAdapter
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.settlement.Settlement
import kotlinx.coroutines.launch

class SettlementsActivity : AppCompatActivity() {

    private lateinit var rvSettlements: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var adapter: SettlementAdapter

    private var groupId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settlements)

        rvSettlements = findViewById(R.id.rvSettlements)
        swipeRefresh = findViewById(R.id.swipeRefreshSettlements)
        tvEmpty = findViewById(R.id.tvEmptySettlements)

        // Get groupId from intent or session
        groupId = intent.getLongExtra("groupId", -1L).takeIf { it != -1L }
            ?: SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        rvSettlements.layoutManager = LinearLayoutManager(this)
        adapter = SettlementAdapter(emptyList(), onMarkPaid = { settlement ->
            markSettlementPaid(settlement)
        })
        rvSettlements.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadSettlements() }

        loadSettlements()
    }

    private fun loadSettlements() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getSettlements(groupId)
                if (response.isSuccessful) {
                    // CHANGE: Access .data from the ApiResponse wrapper
                    val list = response.body()?.data ?: emptyList()
                    adapter.update(list)
                    tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    Toast.makeText(this@SettlementsActivity, "Failed to load settlements", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettlementsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun markSettlementPaid(settlement: Settlement) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.markSettlementPaid(settlement.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@SettlementsActivity, "Marked as paid", Toast.LENGTH_SHORT).show()
                    loadSettlements()
                } else {
                    Toast.makeText(this@SettlementsActivity, "Failed to mark as paid", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettlementsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}