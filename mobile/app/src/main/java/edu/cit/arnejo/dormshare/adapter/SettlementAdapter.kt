package edu.cit.arnejo.dormshare.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.model.Settlement

class SettlementAdapter(
    private var items: List<Settlement>,
    private val onMarkPaid: ((Settlement) -> Unit)? = null
) : RecyclerView.Adapter<SettlementAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSummary: TextView = view.findViewById(R.id.tvSettlementSummary)
        val tvDate: TextView = view.findViewById(R.id.tvSettlementDate)
        val btnMarkPaid: MaterialButton = view.findViewById(R.id.btnMarkPaid)

        init {
            btnMarkPaid.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMarkPaid?.invoke(items[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_settlement, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.tvSummary.text = "${s.fromUserId} → ${s.toUserId}: %.2f".format(s.amount)
        holder.tvDate.text = s.date ?: ""
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Settlement>) {
        items = newItems
        notifyDataSetChanged()
    }
}
