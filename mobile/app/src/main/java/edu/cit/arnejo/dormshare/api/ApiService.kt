package edu.cit.arnejo.dormshare.api

import edu.cit.arnejo.dormshare.model.AuthResponse
import edu.cit.arnejo.dormshare.model.LoginRequest
import edu.cit.arnejo.dormshare.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for DormShare backend authentication endpoints.
 */
interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // Phase 4 - scaffold endpoints for mobile
    @GET("api/groups")
    suspend fun getGroups(): Response<List<edu.cit.arnejo.dormshare.model.Group>>

    @GET("api/expenses/ledger")
    suspend fun getExpenses(@Query("groupId") groupId: Long?): Response<List<edu.cit.arnejo.dormshare.model.Expense>>

    @GET("api/expenses/summary")
    suspend fun getExpenseSummary(@Query("groupId") groupId: Long?): Response<List<edu.cit.arnejo.dormshare.model.Expense>>

    @GET("api/settlements")
    suspend fun getSettlements(@Query("groupId") groupId: Long?): Response<List<edu.cit.arnejo.dormshare.model.Settlement>>

    @POST("api/expenses")
    suspend fun postExpense(@Body expense: edu.cit.arnejo.dormshare.model.Expense, @Query("groupId") groupId: Long?): Response<edu.cit.arnejo.dormshare.model.Expense>
}
