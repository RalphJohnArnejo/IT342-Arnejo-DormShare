package edu.cit.arnejo.dormshare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import edu.cit.arnejo.dormshare.adapter.ExpenseAdapter
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.model.Expense
import kotlinx.coroutines.launch


class ExpensesActivity : AppCompatActivity() {

    private lateinit var rvExpenses: RecyclerView
    private lateinit var fabAddExpense: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        rvExpenses = findViewById(R.id.rvExpenses)
        fabAddExpense = findViewById(R.id.fabAddExpense)

        rvExpenses.layoutManager = LinearLayoutManager(this)
        val adapter = ExpenseAdapter(emptyList())
        rvExpenses.adapter = adapter

        fabAddExpense.setOnClickListener {
            // TODO: open add expense dialog/screen
        }

        // Load expenses
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExpenses(null)
                if (response.isSuccessful) {
                    val list: List<Expense> = response.body() ?: emptyList()
                    adapter.update(list)
                }
            } catch (_: Exception) {
                // ignore for now
            }
        }
    }
}
