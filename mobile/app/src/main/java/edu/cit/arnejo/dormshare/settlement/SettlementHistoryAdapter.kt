package edu.cit.arnejo.dormshare.settlement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R

/**
 * Adapter for the History tab — shows completed settlement transactions.
 */
class SettlementHistoryAdapter(
    private var items: List<SettlementHistoryItem>
) : RecyclerView.Adapter<SettlementHistoryAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvFrom: TextView = view.findViewById(R.id.tvHistoryFrom)
        val tvTo: TextView = view.findViewById(R.id.tvHistoryTo)
        val tvAmount: TextView = view.findViewById(R.id.tvHistoryAmount)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settlement_history, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvFrom.text = item.payerName ?: "User"
        holder.tvTo.text = item.payeeName ?: "User"
        holder.tvAmount.text = "₱${"%.2f".format(item.amount)}"
        holder.tvDate.text = item.createdAt?.let {
            try { it.substringBefore("T") } catch (_: Exception) { "—" }
        } ?: "—"
        holder.tvStatus.text = item.status ?: "SETTLED"
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SettlementHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
