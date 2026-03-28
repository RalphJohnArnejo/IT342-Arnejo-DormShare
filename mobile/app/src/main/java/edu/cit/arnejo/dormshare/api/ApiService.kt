package edu.cit.arnejo.dormshare.api

import edu.cit.arnejo.dormshare.model.AuthResponse
import edu.cit.arnejo.dormshare.model.LoginRequest
import edu.cit.arnejo.dormshare.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for DormShare backend authentication endpoints.
 */
interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
