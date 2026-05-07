package edu.cit.arnejo.dormshare.api

import edu.cit.arnejo.dormshare.model.AuthResponse
import edu.cit.arnejo.dormshare.model.LoginRequest
import edu.cit.arnejo.dormshare.model.RegisterRequest
import edu.cit.arnejo.dormshare.model.Group
import edu.cit.arnejo.dormshare.model.Expense
import edu.cit.arnejo.dormshare.model.Settlement
import edu.cit.arnejo.dormshare.model.PantryItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Path

/**
 * Retrofit API interface for DormShare backend endpoints.
 */
interface ApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // Groups
    @GET("api/groups")
    suspend fun getGroups(): Response<List<Group>>

    @POST("api/groups")
    suspend fun createGroup(@Body group: Group): Response<Group>

    @POST("api/groups/join")
    suspend fun joinGroup(@Body code: Map<String, String>): Response<Group>

    @DELETE("api/groups/{id}/leave")
    suspend fun leaveGroup(@Path("id") id: Long): Response<Void>

    // Expenses
    @GET("api/expenses/ledger")
    suspend fun getExpenses(@Query("groupId") groupId: Long?): Response<List<Expense>>

    @GET("api/expenses/summary")
    suspend fun getExpenseSummary(@Query("groupId") groupId: Long?): Response<List<Expense>>

    @POST("api/expenses")
    suspend fun createExpense(@Body expense: Expense, @Query("groupId") groupId: Long?): Response<Expense>

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: Long, @Body expense: Expense): Response<Expense>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Long): Response<Void>

    // Settlements
    @GET("api/settlements")
    suspend fun getSettlements(@Query("groupId") groupId: Long?): Response<List<Settlement>>

    @POST("api/settlements")
    suspend fun createSettlement(@Body settlement: Settlement, @Query("groupId") groupId: Long?): Response<Settlement>

    @PUT("api/settlements/{id}/pay")
    suspend fun markSettlementPaid(@Path("id") id: Long): Response<Settlement>

    // Pantry
    @GET("api/pantry")
    suspend fun getPantryItems(@Query("groupId") groupId: Long?): Response<List<PantryItem>>

    @POST("api/pantry")
    suspend fun createPantryItem(@Body item: PantryItem, @Query("groupId") groupId: Long?): Response<PantryItem>

    @PUT("api/pantry/{id}")
    suspend fun updatePantryItem(@Path("id") id: Long, @Body item: PantryItem): Response<PantryItem>

    @DELETE("api/pantry/{id}")
    suspend fun deletePantryItem(@Path("id") id: Long): Response<Void>

    @PUT("api/pantry/{id}/status")
    suspend fun updatePantryStatus(@Path("id") id: Long, @Body status: Map<String, String>): Response<PantryItem>
}
