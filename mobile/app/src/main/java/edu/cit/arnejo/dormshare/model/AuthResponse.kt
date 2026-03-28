package edu.cit.arnejo.dormshare.model

/**
 * Matches the backend AuthResponse structure.
 *
 * Success: { success: true, data: { token, userId, email, firstName, lastName, role }, timestamp }
 * Error:   { success: false, error: { code, message, details }, timestamp }
 */
data class AuthResponse(
    val success: Boolean,
    val data: Map<String, Any>?,
    val error: ErrorDetail?,
    val timestamp: String?
)

data class ErrorDetail(
    val code: String?,
    val message: String?,
    val details: Any?
)
