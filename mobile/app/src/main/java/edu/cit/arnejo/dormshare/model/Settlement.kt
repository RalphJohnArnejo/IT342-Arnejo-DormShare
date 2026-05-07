package edu.cit.arnejo.dormshare.model

data class Settlement(
    val id: Long,
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double,
    val date: String?
)
