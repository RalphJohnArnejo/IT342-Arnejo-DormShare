package edu.cit.arnejo.dormshare.settlement

/**
 * A single debt entry from GET /api/ledger/summary.
 * The backend returns { debts: [...] } where each debt has this structure.
 */
data class SettlementDebt(
    val splitId: Long? = null,
    val fromUserId: Long = 0,
    val fromUserName: String? = null,
    val toUserId: Long = 0,
    val toUserName: String? = null,
    val amount: Double = 0.0,
    val status: String? = null,
    val createdAt: String? = null
)
