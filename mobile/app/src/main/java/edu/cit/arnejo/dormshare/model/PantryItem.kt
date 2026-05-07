package edu.cit.arnejo.dormshare.model

data class PantryItem(
    val id: Long,
    val name: String,
    val quantity: Int,
    val category: String,
    val status: String, // "in_stock", "low_stock", "out_of_stock"
    val groupId: Long?,
    val updatedBy: String?,
    val updatedAt: String?
)
