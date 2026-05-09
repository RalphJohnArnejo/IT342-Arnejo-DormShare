package edu.cit.arnejo.dormshare.settlement

data class Settlement(
    val id: Long,
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double,
    val date: String?
)
