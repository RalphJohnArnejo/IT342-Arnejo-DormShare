package edu.cit.arnejo.dormshare

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.adapter.PantryAdapter
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.auth.SessionManager
import edu.cit.arnejo.dormshare.databinding.ActivityPantryBinding
import edu.cit.arnejo.dormshare.model.PantryItem
import kotlinx.coroutines.launch

class PantryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPantryBinding
    private lateinit var adapter: PantryAdapter
    private var groupId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra("groupId", -1L).takeIf { it != -1L }
            ?: SessionManager.getSelectedGroupId(this).takeIf { it != -1L }

        adapter = PantryAdapter(
            emptyList(),
            onStatusChange = { item, status -> updateStatus(item, status) },
            onEdit = { item -> showEditDialog(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        binding.rvPantry.layoutManager = LinearLayoutManager(this)
        binding.rvPantry.adapter = adapter

        binding.btnAddItem.setOnClickListener { showAddDialog() }

        // Filter chips
        binding.filterAll.setOnClickListener { loadPantryItems(null) }
        binding.filterInStock.setOnClickListener { loadPantryItems("in_stock") }
        binding.filterLowStock.setOnClickListener { loadPantryItems("low_stock") }
        binding.filterOutOfStock.setOnClickListener { loadPantryItems("out_of_stock") }

        loadPantryItems(null)
    }

    private fun loadPantryItems(statusFilter: String?) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPantryItems(groupId)
                if (response.isSuccessful) {
                    var list = response.body() ?: emptyList()
                    if (statusFilter != null) {
                        list = list.filter { it.status == statusFilter }
                    }
                    adapter.update(list)
                    updateSummary(response.body() ?: emptyList())
                    binding.tvEmptyPantry.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary(items: List<PantryItem>) {
        binding.tvTotalItems.text = items.size.toString()
        binding.tvInStock.text = items.count { it.status == "in_stock" }.toString()
        binding.tvLowStock.text = items.count { it.status == "low_stock" }.toString()
        binding.tvOutOfStock.text = items.count { it.status == "out_of_stock" }.toString()
    }

    private fun showAddDialog() {
        val nameInput = TextInputEditText(this).apply { hint = "Item name (e.g. Egg)" }
        val qtyInput = TextInputEditText(this).apply { hint = "Quantity" }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(nameInput)
            addView(qtyInput)
        }
        container.setPadding(40, 0, 40, 40)

        AlertDialog.Builder(this)
            .setTitle("Add Pantry Item")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val qty = qtyInput.text.toString().trim().toIntOrNull() ?: 1
                if (name.isNotEmpty()) {
                    addItem(name, qty)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addItem(name: String, quantity: Int) {
        val item = PantryItem(0, name, quantity, "Other", "in_stock", groupId, SessionManager.getUserName(this), null)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createPantryItem(item, groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Item added!", Toast.LENGTH_SHORT).show()
                    loadPantryItems(null)
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus(item: PantryItem, status: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updatePantryStatus(item.id, mapOf("status" to status))
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Status updated!", Toast.LENGTH_SHORT).show()
                    loadPantryItems(null)
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(item: PantryItem) {
        val nameInput = TextInputEditText(this).apply { setText(item.name) }
        val qtyInput = TextInputEditText(this).apply { setText(item.quantity.toString()) }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(nameInput)
            addView(qtyInput)
        }
        container.setPadding(40, 0, 40, 40)

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val qty = qtyInput.text.toString().trim().toIntOrNull() ?: 1
                if (name.isNotEmpty()) {
                    updateItem(item.id, name, qty)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateItem(id: Long, name: String, quantity: Int) {
        val item = PantryItem(id, name, quantity, "Other", "in_stock", groupId, SessionManager.getUserName(this), null)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updatePantryItem(id, item)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Updated!", Toast.LENGTH_SHORT).show()
                    loadPantryItems(null)
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(item: PantryItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete ${item.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(item.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(id: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deletePantryItem(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Deleted!", Toast.LENGTH_SHORT).show()
                    loadPantryItems(null)
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
