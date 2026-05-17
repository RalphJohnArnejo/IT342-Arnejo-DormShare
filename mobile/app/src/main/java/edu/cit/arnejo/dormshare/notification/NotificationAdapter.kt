package edu.cit.arnejo.dormshare.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R

/**
 * Adapter for displaying notifications with an unread dot indicator.
 */
class NotificationAdapter(
    private var items: List<Notification>,
    private val onItemClick: ((Notification) -> Unit)? = null
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val root: LinearLayout = view.findViewById(R.id.notificationRoot)
        val unreadDot: View = view.findViewById(R.id.viewUnreadDot)
        val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvBody: TextView = view.findViewById(R.id.tvNotificationBody)
        val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title ?: "Notification"
        holder.tvBody.text = item.body ?: ""
        holder.tvBody.visibility = if (item.body.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.unreadDot.visibility = if (!item.isRead) View.VISIBLE else View.INVISIBLE

        holder.tvTime.text = item.createdAt?.let {
            try { it.substringBefore("T") } catch (_: Exception) { "" }
        } ?: ""

        // Dim read notifications
        holder.root.alpha = if (item.isRead) 0.6f else 1.0f

        holder.root.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Notification>) {
        items = newItems
        notifyDataSetChanged()
    }
}
