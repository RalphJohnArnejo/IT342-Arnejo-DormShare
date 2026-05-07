package edu.cit.arnejo.dormshare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import edu.cit.arnejo.dormshare.adapter.GroupAdapter
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.model.Group
import kotlinx.coroutines.launch

class GroupsActivity : AppCompatActivity() {

    private lateinit var rvGroups: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        rvGroups = findViewById(R.id.rvGroups)
        rvGroups.layoutManager = LinearLayoutManager(this)
        val adapter = GroupAdapter(emptyList())
        rvGroups.adapter = adapter

        // Load groups
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups()
                if (response.isSuccessful) {
                    val list: List<Group> = response.body() ?: emptyList()
                    adapter.update(list)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
