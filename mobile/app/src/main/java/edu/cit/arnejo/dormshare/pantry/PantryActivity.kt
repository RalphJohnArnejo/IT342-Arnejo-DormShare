package edu.cit.arnejo.dormshare.pantry

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.databinding.ActivityPantryBinding
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantry screen — enhanced to match web Pantry.jsx.
 * Adds: category spinner in add/edit dialogs, search bar, status change via PATCH,
 * proper status filter values matching backend.
 */
class PantryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPantryBinding
    private lateinit var adapter: PantryAdapter
    private var groupId: Long? = null
    private var allItems: List<PantryItem> = emptyList()
    private var currentFilter: String? = null
    private var searchJob: Job? = null

    companion object {
        val CATEGORIES = listOf(
            "Other", "Groceries", "Beverages", "Snacks",
            "Cleaning", "Personal Care", "Condiments", "Frozen"
        )
    }

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
        binding.filterAll.setOnClickListener { applyFilter(null) }
        binding.filterInStock.setOnClickListener { applyFilter("in_stock") }
        binding.filterLowStock.setOnClickListener { applyFilter("low_stock") }
        binding.filterOutOfStock.setOnClickListener { applyFilter("out_of_stock") }

        // Search bar (if present in layout)
        try {
            val etSearch = findViewById<EditText>(R.id.etSearch)
            etSearch?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    searchJob?.cancel()
                    if (query.isEmpty()) {
                        applyFilter(currentFilter)
                    } else {
                        searchJob = lifecycleScope.launch {
                            delay(300) // debounce
                            searchItems(query)
                        }
                    }
                }
            })
        } catch (_: Exception) {}

        // SwipeRefresh if present
        try {
            binding.swipeRefreshPantry.setOnRefreshListener { loadPantryItems() }
            binding.swipeRefreshPantry.setColorSchemeColors(0xFFC49A3C.toInt())
        } catch (_: Exception) {}

        loadPantryItems()
    }

    private fun loadPantryItems() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPantryItems(groupId)
                if (response.isSuccessful) {
                    allItems = response.body()?.data ?: emptyList()
                    applyFilter(currentFilter)
                    updateSummary(allItems)
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                try { binding.swipeRefreshPantry.isRefreshing = false } catch (_: Exception) {}
            }
        }
    }

    private fun applyFilter(filter: String?) {
        currentFilter = filter
        val filtered = if (filter != null) {
            allItems.filter { it.status == filter }
        } else {
            allItems
        }
        adapter.update(filtered)
        binding.tvEmptyPantry.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun searchItems(query: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.searchPantryItems(query, groupId)
                if (response.isSuccessful) {
                    val results = response.body()?.data ?: emptyList()
                    adapter.update(results)
                    binding.tvEmptyPantry.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateSummary(items: List<PantryItem>) {
        binding.tvTotalItems.text = items.size.toString()
        binding.tvInStock.text = items.count { it.status == "in_stock" }.toString()
        binding.tvLowStock.text = items.count { it.status == "low_stock" }.toString()
        binding.tvOutOfStock.text = items.count { it.status == "out_of_stock" }.toString()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pantry_item, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etQty = dialogView.findViewById<TextInputEditText>(R.id.etItemQuantity)
        val spinnerCat = dialogView.findViewById<Spinner>(R.id.spinnerItemCategory)

        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, CATEGORIES)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCat.adapter = catAdapter

        AlertDialog.Builder(this)
            .setTitle("Add Pantry Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val qty = etQty.text.toString().trim().toIntOrNull() ?: 1
                val category = CATEGORIES[spinnerCat.selectedItemPosition]
                if (name.isNotEmpty()) {
                    addItem(name, qty, category)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addItem(name: String, quantity: Int, category: String) {
        val item = PantryItem(0, name, quantity, category, "in_stock", groupId, SessionManager.getUserName(this), null)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createPantryItem(item, groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Item added!", Toast.LENGTH_SHORT).show()
                    loadPantryItems()
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Status change now goes through PATCH /api/pantry/{id} — matching the backend.
     * The old code called a non-existent updatePantryStatus endpoint.
     */
    private fun updateStatus(item: PantryItem, status: String) {
        val updated = PantryItem(item.id, item.name, item.quantity, item.category, status, item.groupId, item.updatedBy, item.updatedAt)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updatePantryItem(item.id, updated)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Status updated!", Toast.LENGTH_SHORT).show()
                    loadPantryItems()
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(item: PantryItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pantry_item, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etQty = dialogView.findViewById<TextInputEditText>(R.id.etItemQuantity)
        val spinnerCat = dialogView.findViewById<Spinner>(R.id.spinnerItemCategory)

        etName.setText(item.name)
        etQty.setText(item.quantity.toString())

        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, CATEGORIES)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCat.adapter = catAdapter

        val catIdx = CATEGORIES.indexOf(item.category).takeIf { it >= 0 } ?: 0
        spinnerCat.setSelection(catIdx)

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etName.text.toString().trim()
                val qty = etQty.text.toString().trim().toIntOrNull() ?: 1
                val category = CATEGORIES[spinnerCat.selectedItemPosition]
                if (name.isNotEmpty()) {
                    updateItem(item, name, qty, category)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateItem(oldItem: PantryItem, name: String, quantity: Int, category: String) {
        val updated = PantryItem(oldItem.id, name, quantity, category, oldItem.status, oldItem.groupId, SessionManager.getUserName(this), null)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updatePantryItem(oldItem.id, updated)
                if (response.isSuccessful) {
                    Toast.makeText(this@PantryActivity, "Updated!", Toast.LENGTH_SHORT).show()
                    loadPantryItems()
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
                    loadPantryItems()
                } else {
                    Toast.makeText(this@PantryActivity, "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PantryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}