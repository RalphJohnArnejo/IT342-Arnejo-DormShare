package edu.cit.arnejo.dormshare.expense

/**
 * Represents a single split within an expense.
 * Maps to the backend ExpenseSplit entity returned in expense ledger responses.
 */
data class ExpenseSplit(
    val id: Long = 0,
    val userId: Long = 0,
    val userName: String? = null,
    val amountOwed: Double = 0.0,
    val isSettled: Boolean = false
)
