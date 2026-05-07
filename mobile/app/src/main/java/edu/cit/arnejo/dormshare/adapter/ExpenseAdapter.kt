package edu.cit.arnejo.dormshare.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.databinding.ItemExpenseBinding
import edu.cit.arnejo.dormshare.model.Expense

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onItemLongClick: ((Expense) -> Boolean)? = null,
    private val onSettleClick: ((Expense) -> Unit)? = null
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val binding = holder.binding

        binding.tvExpenseDesc.text = expense.description ?: "Expense"
        binding.tvExpenseCategory.text = "Groceries"
        binding.tvExpenseAmount.text = "₱%.0f".format(expense.amount)
        binding.tvExpensePaidBy.text = "Paid by • ${expense.date ?: ""}"

        // Show split info
        val splitAmount = expense.amount / 2
        binding.tvSplitPaid.text = "You: ₱%.0f ✓".format(splitAmount)
        binding.tvSplitOwe.text = "Roommate: ₱%.0f SETTLE".format(splitAmount)

        binding.tvSplitOwe.setOnClickListener {
            onSettleClick?.invoke(expense)
        }

        binding.root.setOnLongClickListener {
            onItemLongClick?.invoke(expense) ?: false
        }
    }

    override fun getItemCount() = expenses.size

    fun update(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
