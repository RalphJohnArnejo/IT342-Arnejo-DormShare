package edu.cit.arnejo.dormshare.model

data class ExpenseSummary(
    val owedToYou: Double = 0.0,
    val youOwe: Double = 0.0
) {
    val netBalance: Double
        get() = owedToYou - youOwe
}
