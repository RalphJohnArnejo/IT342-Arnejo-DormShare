package edu.cit.arnejo.dormshare.expense

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.databinding.ItemExpenseBinding

/**
 * Updated adapter that displays real split data, categories, and payer info
 * from the backend — matching the web Expenses.jsx ledger display.
 */
class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val currentUserId: Long = 0,
    private val onItemLongClick: ((Expense) -> Boolean)? = null,
    private val onSettleClick: ((Long) -> Unit)? = null // splitId
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
        binding.tvExpenseCategory.text = expense.category ?: "General"
        binding.tvExpenseAmount.text = "₱%.2f".format(expense.amount)
        binding.tvExpensePaidBy.text = buildString {
            append("Paid by ")
            append(expense.payerName ?: "Unknown")
            if (expense.date != null) {
                append(" • ")
                append(expense.date)
            }
        }

        // Show split info from real data
        val splits = expense.splits ?: emptyList()
        if (splits.isNotEmpty()) {
            val splitTexts = splits.map { split ->
                val name = split.userName ?: "User"
                val amount = "₱%.0f".format(split.amountOwed)
                val status = if (split.isSettled) " ✓" else ""
                "$name: $amount$status"
            }
            binding.tvSplitPaid.text = splitTexts.firstOrNull() ?: ""
            binding.tvSplitOwe.text = if (splitTexts.size > 1) {
                splitTexts.drop(1).joinToString(" | ")
            } else ""

            // Find first unsettled split that belongs to someone else (payer can settle for them)
            val unsettled = splits.firstOrNull {
                !it.isSettled && it.userId != currentUserId && expense.paidById == currentUserId
            }
            if (unsettled != null) {
                binding.tvSplitOwe.setOnClickListener {
                    onSettleClick?.invoke(unsettled.id)
                }
            }
        } else {
            // Fallback when no splits returned
            binding.tvSplitPaid.text = ""
            binding.tvSplitOwe.text = ""
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
