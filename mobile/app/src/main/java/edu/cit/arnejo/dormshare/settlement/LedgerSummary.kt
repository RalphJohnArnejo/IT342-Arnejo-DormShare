package edu.cit.arnejo.dormshare.settlement

/**
 * Wrapper for the GET /api/ledger/summary response.
 * Backend returns: { success: true, data: { debts: [...], totalOwed: ..., totalOwing: ... } }
 */
data class LedgerSummary(
    val debts: List<SettlementDebt> = emptyList(),
    val totalOwed: Double = 0.0,
    val totalOwing: Double = 0.0
)
