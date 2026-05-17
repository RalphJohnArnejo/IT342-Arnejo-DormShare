package edu.cit.arnejo.dormshare.settlement

/**
 * A completed settlement entry from GET /api/ledger/history.
 */
data class SettlementHistoryItem(
    val id: Long = 0,
    val payerName: String? = null,
    val payeeName: String? = null,
    val payerId: Long = 0,
    val payeeId: Long = 0,
    val amount: Double = 0.0,
    val status: String? = null,
    val createdAt: String? = null
)
