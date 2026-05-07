package edu.cit.arnejo.dormshare.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.databinding.ItemPantryBinding
import edu.cit.arnejo.dormshare.model.PantryItem

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
        binding.tvQuantity.text = item.quantity.toString()
        binding.tvCategory.text = item.category
        binding.tvUpdatedBy.text = "Updated by ${item.updatedBy ?: "User"}"
        binding.tvUpdatedAt.text = item.updatedAt ?: "Just now"

        // Status badge
        when (item.status) {
            "in_stock" -> {
                binding.tvStatusBadge.text = "IN STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_green)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.green_success))
            }
            "low_stock" -> {
                binding.tvStatusBadge.text = "LOW STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_amber)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.amber_warning))
            }
            "out_of_stock" -> {
                binding.tvStatusBadge.text = "OUT OF STOCK"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_gray)
                binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(R.color.red_error))
            }
        }

        // Status buttons
        binding.btnSetIn.setOnClickListener { onStatusChange?.invoke(item, "in_stock") }
        binding.btnSetLow.setOnClickListener { onStatusChange?.invoke(item, "low_stock") }
        binding.btnSetOut.setOnClickListener { onStatusChange?.invoke(item, "out_of_stock") }

        binding.btnEditItem.setOnClickListener { onEdit?.invoke(item) }
        binding.btnDeleteItem.setOnClickListener { onDelete?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<PantryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
