package edu.cit.arnejo.dormshare.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.model.Expense

class ExpenseAdapter(private var items: List<Expense>) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvDescription.text = e.description ?: "Expense"
        holder.tvMeta.text = "Amount: ${e.amount} • By: ${e.paidById}"
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Expense>) {
        items = newItems
        notifyDataSetChanged()
    }
}
