package edu.cit.arnejo.dormshare.profile

/**
 * Matches the backend GET /api/users/me response data structure.
 */
data class UserProfile(
    val id: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val createdAt: String? = null
)
