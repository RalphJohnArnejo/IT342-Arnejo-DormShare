package edu.cit.arnejo.dormshare.pantry

import com.google.gson.annotations.SerializedName

data class PantryItem(
    val id: Long,
    @SerializedName("itemName") val name: String,
    val quantity: Double,
    val category: String,
    val status: String, // "IN", "LOW", "OUT"
    val groupId: Long?,
    @SerializedName("updatedByName") val updatedBy: String?,
    val updatedAt: String?
)
