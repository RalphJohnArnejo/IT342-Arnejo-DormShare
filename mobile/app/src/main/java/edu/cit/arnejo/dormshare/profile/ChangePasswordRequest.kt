package edu.cit.arnejo.dormshare.profile

/**
 * DTO for POST /api/users/change-password — matches backend ChangePasswordRequest.
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
