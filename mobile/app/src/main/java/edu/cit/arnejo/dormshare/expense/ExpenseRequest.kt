package edu.cit.arnejo.dormshare.expense

/**
 * DTO for creating a new expense with splits.
 * Matches the web app's logExpense() payload exactly.
 */
data class ExpenseRequest(
    val amount: Double,
    val description: String,
    val category: String = "Other",
    val splits: List<SplitEntry> = emptyList()
)

data class SplitEntry(
    val userId: Long,
    val amount: Double
)
