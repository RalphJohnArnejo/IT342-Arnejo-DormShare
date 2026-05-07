package edu.cit.arnejo.dormshare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import edu.cit.arnejo.dormshare.adapter.SettlementAdapter
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.model.Settlement
import kotlinx.coroutines.launch

class SettlementsActivity : AppCompatActivity() {

    private lateinit var rvSettlements: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settlements)

        rvSettlements = findViewById(R.id.rvSettlements)
        rvSettlements.layoutManager = LinearLayoutManager(this)
        val adapter = SettlementAdapter(emptyList())
        rvSettlements.adapter = adapter

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getSettlements(null)
                if (response.isSuccessful) {
                    val list: List<Settlement> = response.body() ?: emptyList()
                    adapter.update(list)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
