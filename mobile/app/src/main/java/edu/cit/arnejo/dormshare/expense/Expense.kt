package edu.cit.arnejo.dormshare.expense

import com.google.gson.annotations.SerializedName

data class Expense(
    val id: Long,
    val description: String?,
    val amount: Double,
    @SerializedName("groupId") val groupId: Long?,
    val date: String?,
    val paidById: Long?
)
