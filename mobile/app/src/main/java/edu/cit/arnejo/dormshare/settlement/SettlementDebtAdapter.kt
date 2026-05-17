package edu.cit.arnejo.dormshare.settlement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import edu.cit.arnejo.dormshare.R

/**
 * Adapter for displaying settlement debts (You Owe / You Receive tabs).
 * Shows user names, amounts, and an optional "Settle" button.
 */
class SettlementDebtAdapter(
    private var items: List<SettlementDebt>,
    private var showPayButton: Boolean = true,
    private val onSettle: ((SettlementDebt) -> Unit)? = null
) : RecyclerView.Adapter<SettlementDebtAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSummary: TextView = view.findViewById(R.id.tvSettlementSummary)
        val tvDate: TextView = view.findViewById(R.id.tvSettlementDate)
        val tvAmount: TextView = view.findViewById(R.id.tvSettlementAmount)
        val tvStatus: TextView = view.findViewById(R.id.tvSettlementStatus)
        val btnMarkPaid: MaterialButton = view.findViewById(R.id.btnMarkPaid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settlement_debt, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val debt = items[position]

        holder.tvSummary.text = if (showPayButton) {
            "Pay ${debt.toUserName ?: "User"}"
        } else {
            "${debt.fromUserName ?: "User"} owes you"
        }

        holder.tvAmount.text = "₱${"%.2f".format(debt.amount)}"
        holder.tvStatus.text = debt.status ?: "PENDING"

        holder.tvDate.text = debt.createdAt?.let {
            try { "Since ${it.substringBefore("T")}" } catch (_: Exception) { "Outstanding" }
        } ?: "Outstanding"

        if (showPayButton && debt.splitId != null) {
            holder.btnMarkPaid.visibility = View.VISIBLE
            holder.btnMarkPaid.setOnClickListener { onSettle?.invoke(debt) }
        } else {
            holder.btnMarkPaid.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SettlementDebt>, showPayButton: Boolean = this.showPayButton) {
        items = newItems
        this.showPayButton = showPayButton
        notifyDataSetChanged()
    }
}
