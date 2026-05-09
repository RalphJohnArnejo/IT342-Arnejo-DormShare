package edu.cit.arnejo.dormshare.auth

/**
 * Matches the backend's flat login/register response data structure:
 * { "token": "...", "userId": 1, "email": "...", "firstName": "...", "lastName": "...", "role": "..." }
 */
data class AuthResponse(
    val token: String,
    val userId: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)