package edu.cit.arnejo.dormshare.notification

/**
 * Matches the backend NotificationEntity structure.
 */
data class Notification(
    val id: Long,
    val title: String? = null,
    val body: String? = null,
    val isRead: Boolean = false,
    val createdAt: String? = null,
    val type: String? = null
)
