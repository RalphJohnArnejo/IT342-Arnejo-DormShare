package edu.cit.arnejo.dormshare.shared.api

import edu.cit.arnejo.dormshare.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for DormShare backend endpoints.
 */
interface ApiService {

    // Auth - Wrapped in ApiResponse to match the backend structure
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    // Groups
    @GET("api/groups/my")
    suspend fun getGroups(): Response<ApiResponse<List<Group>>>

    @POST("api/groups")
    suspend fun createGroup(@Body group: Group): Response<ApiResponse<Group>>

    @POST("api/groups/join")
    suspend fun joinGroup(@Body code: Map<String, String>): Response<ApiResponse<Group>>

    // Wrapped delete/leave methods if backend returns an ApiResponse object
    @DELETE("api/groups/leave/{id}")
    suspend fun leaveGroup(@Path("id") id: Long): Response<ApiResponse<Void>>

    // Expenses
    @GET("api/expenses/ledger")
    suspend fun getExpenses(@Query("groupId") groupId: Long?): Response<ApiResponse<List<Expense>>>

    @GET("api/expenses/summary")
    suspend fun getExpenseSummary(@Query("groupId") groupId: Long?): Response<ApiResponse<ExpenseSummary>>

    @POST("api/expenses")
    suspend fun createExpense(@Body expense: Expense, @Query("groupId") groupId: Long?): Response<ApiResponse<Expense>>

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: Long, @Body expense: Expense): Response<ApiResponse<Expense>>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Long): Response<ApiResponse<Void>>

    // Settlements
    @GET("api/settlements")
    suspend fun getSettlements(@Query("groupId") groupId: Long?): Response<ApiResponse<List<Settlement>>>

    @POST("api/settlements")
    suspend fun createSettlement(@Body settlement: Settlement, @Query("groupId") groupId: Long?): Response<ApiResponse<Settlement>>

    @PUT("api/settlements/{id}/pay")
    suspend fun markSettlementPaid(@Path("id") id: Long): Response<ApiResponse<Settlement>>

    // Pantry
    @GET("api/pantry")
    suspend fun getPantryItems(@Query("groupId") groupId: Long?): Response<ApiResponse<List<PantryItem>>>

    @POST("api/pantry")
    suspend fun createPantryItem(@Body item: PantryItem, @Query("groupId") groupId: Long?): Response<ApiResponse<PantryItem>>

    @PUT("api/pantry/{id}")
    suspend fun updatePantryItem(@Path("id") id: Long, @Body item: PantryItem): Response<ApiResponse<PantryItem>>

    @DELETE("api/pantry/{id}")
    suspend fun deletePantryItem(@Path("id") id: Long): Response<ApiResponse<Void>>

    @PUT("api/pantry/{id}/status")
    suspend fun updatePantryStatus(@Path("id") id: Long, @Body status: Map<String, String>): Response<ApiResponse<PantryItem>>
}