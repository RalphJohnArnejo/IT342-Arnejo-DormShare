package edu.cit.arnejo.dormshare.profile

/**
 * DTO for PATCH /api/users/me — matches backend UpdateProfileRequest.
 */
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
)
