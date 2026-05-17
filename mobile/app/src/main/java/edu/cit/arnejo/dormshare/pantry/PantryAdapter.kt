package edu.cit.arnejo.dormshare.pantry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.databinding.ItemPantryBinding
import edu.cit.arnejo.dormshare.pantry.PantryItem

class PantryAdapter(
    private var items: List<PantryItem>,
    private val onStatusChange: ((PantryItem, String) -> Unit)? = null,
    private val onEdit: ((PantryItem) -> Unit)? = null,
    private val onDelete: ((PantryItem) -> Unit)? = null
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    class PantryViewHolder(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PantryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.tvItemName.text = item.name
        // Display quantity as integer if it's whole, otherwise with decimal
        val qty = item.quantity
        binding.tvQuantity.text = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
        binding.tvCategory.text = item.category
        binding.tvUpdatedBy.text = "Updated by ${item.updatedBy ?: "User"}"
        binding.tvUpdatedAt.text = item.updatedAt ?: "Just now"

        // Status badge — backend uses IN/LOW/OUT
        when (item.status) {
            "IN" -> {
                binding.tvStatusBadge.text = "IN STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_green)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.green_success))
            }
            "LOW" -> {
                binding.tvStatusBadge.text = "LOW STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_amber)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.amber_warning))
            }
            "OUT" -> {
                binding.tvStatusBadge.text = "OUT OF STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_gray)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.red_error))
            }
        }

        // Status buttons — send backend-compatible values
        binding.btnSetIn.setOnClickListener { onStatusChange?.invoke(item, "IN") }
        binding.btnSetLow.setOnClickListener { onStatusChange?.invoke(item, "LOW") }
        binding.btnSetOut.setOnClickListener { onStatusChange?.invoke(item, "OUT") }

        binding.btnEditItem.setOnClickListener { onEdit?.invoke(item) }
        binding.btnDeleteItem.setOnClickListener { onDelete?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<PantryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
