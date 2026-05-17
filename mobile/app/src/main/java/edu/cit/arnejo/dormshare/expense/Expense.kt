package edu.cit.arnejo.dormshare.expense

import com.google.gson.annotations.SerializedName

/**
 * Matches the backend expense ledger response structure.
 * Includes splits array and payer info that the web app relies on.
 */
data class Expense(
    val id: Long = 0,
    val description: String? = null,
    val amount: Double = 0.0,
    val category: String? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    val date: String? = null,
    val paidById: Long? = null,
    val payerName: String? = null,
    val splits: List<ExpenseSplit>? = null
)
