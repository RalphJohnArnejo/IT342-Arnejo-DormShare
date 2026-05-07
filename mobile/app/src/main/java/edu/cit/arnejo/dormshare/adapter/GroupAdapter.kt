package edu.cit.arnejo.dormshare.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.model.Group

class GroupAdapter(private var items: List<Group>) : RecyclerView.Adapter<GroupAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvGroupDesc: TextView = view.findViewById(R.id.tvGroupDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = items[position]
        holder.tvGroupName.text = g.name
        holder.tvGroupDesc.text = g.description ?: ""
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Group>) {
        items = newItems
        notifyDataSetChanged()
    }
}
