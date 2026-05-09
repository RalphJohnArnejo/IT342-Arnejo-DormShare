package edu.cit.arnejo.dormshare.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError? = null
)

data class ApiError(
    val code: String?,    // Added
    val message: String?,
    val details: Any? = null // Added
)