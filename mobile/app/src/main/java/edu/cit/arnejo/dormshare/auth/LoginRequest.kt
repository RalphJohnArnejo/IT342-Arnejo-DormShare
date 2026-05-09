package edu.cit.arnejo.dormshare.auth

data class LoginRequest(
    val email: String,
    val password: String
)
