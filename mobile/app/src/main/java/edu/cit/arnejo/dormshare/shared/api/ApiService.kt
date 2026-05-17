package edu.cit.arnejo.dormshare.shared.api

import edu.cit.arnejo.dormshare.auth.AuthResponse
import edu.cit.arnejo.dormshare.auth.LoginRequest
import edu.cit.arnejo.dormshare.auth.RegisterRequest
import edu.cit.arnejo.dormshare.expense.Expense
import edu.cit.arnejo.dormshare.expense.ExpenseRequest
import edu.cit.arnejo.dormshare.expense.ExpenseSummary
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.notification.Notification
import edu.cit.arnejo.dormshare.pantry.PantryItem
import edu.cit.arnejo.dormshare.profile.ChangePasswordRequest
import edu.cit.arnejo.dormshare.profile.UpdateProfileRequest
import edu.cit.arnejo.dormshare.profile.UserProfile
import edu.cit.arnejo.dormshare.settlement.LedgerSummary
import edu.cit.arnejo.dormshare.settlement.SettlementHistoryItem
import edu.cit.arnejo.dormshare.shared.model.ApiResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for DormShare backend endpoints.
 * Mirrors every endpoint consumed by the web app (api.js).
 */
interface ApiService {

    // ==================== AUTH ENDPOINTS ====================

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    // ==================== GROUP ENDPOINTS ====================

    @GET("api/groups/my")
    suspend fun getGroups(): Response<ApiResponse<List<Group>>>

    @GET("api/groups/{groupId}")
    suspend fun getGroupById(@Path("groupId") groupId: Long): Response<ApiResponse<Group>>

    @POST("api/groups")
    suspend fun createGroup(@Body body: Map<String, String>): Response<ApiResponse<Group>>

    @POST("api/groups/join")
    suspend fun joinGroup(@Body code: Map<String, String>): Response<ApiResponse<Group>>

    @DELETE("api/groups/leave/{id}")
    suspend fun leaveGroup(@Path("id") id: Long): Response<ApiResponse<Void>>

    // ==================== EXPENSE ENDPOINTS ====================

    @GET("api/expenses/ledger")
    suspend fun getExpenseLedger(@Query("groupId") groupId: Long? = null): Response<ApiResponse<List<Expense>>>

    @GET("api/expenses/summary")
    suspend fun getExpenseSummary(@Query("groupId") groupId: Long? = null): Response<ApiResponse<ExpenseSummary>>

    @GET("api/expenses/users")
    suspend fun getRoommates(): Response<ApiResponse<List<Map<String, Any>>>>

    @POST("api/expenses")
    suspend fun createExpense(
        @Body expense: ExpenseRequest,
        @Query("groupId") groupId: Long? = null
    ): Response<ApiResponse<Expense>>

    @PATCH("api/expenses/settle/{splitId}")
    suspend fun settleSplit(@Path("splitId") splitId: Long): Response<ApiResponse<Any>>

    // ==================== PANTRY ENDPOINTS ====================

    @GET("api/pantry")
    suspend fun getPantryItems(@Query("groupId") groupId: Long? = null): Response<ApiResponse<List<PantryItem>>>

    @GET("api/pantry/stats")
    suspend fun getPantryStats(@Query("groupId") groupId: Long? = null): Response<ApiResponse<Map<String, Int>>>

    @GET("api/pantry/{id}")
    suspend fun getPantryItemById(@Path("id") id: Long): Response<ApiResponse<PantryItem>>

    @GET("api/pantry/status/{status}")
    suspend fun getPantryItemsByStatus(
        @Path("status") status: String,
        @Query("groupId") groupId: Long? = null
    ): Response<ApiResponse<List<PantryItem>>>

    @GET("api/pantry/category/{category}")
    suspend fun getPantryItemsByCategory(
        @Path("category") category: String,
        @Query("groupId") groupId: Long? = null
    ): Response<ApiResponse<List<PantryItem>>>

    @GET("api/pantry/search")
    suspend fun searchPantryItems(
        @Query("q") query: String,
        @Query("groupId") groupId: Long? = null
    ): Response<ApiResponse<List<PantryItem>>>

    @POST("api/pantry")
    suspend fun createPantryItem(
        @Body item: PantryItem,
        @Query("groupId") groupId: Long? = null
    ): Response<ApiResponse<PantryItem>>

    @PATCH("api/pantry/{id}")
    suspend fun updatePantryItem(
        @Path("id") id: Long,
        @Body item: PantryItem
    ): Response<ApiResponse<PantryItem>>

    @DELETE("api/pantry/{id}")
    suspend fun deletePantryItem(@Path("id") id: Long): Response<ApiResponse<Void>>

    // ==================== SETTLEMENT & LEDGER ENDPOINTS ====================

    @GET("api/ledger/summary")
    suspend fun getLedgerSummary(@Query("groupId") groupId: Long? = null): Response<ApiResponse<LedgerSummary>>

    @GET("api/ledger/history")
    suspend fun getLedgerHistory(@Query("groupId") groupId: Long? = null): Response<ApiResponse<List<SettlementHistoryItem>>>

    @POST("api/payments/initiate")
    suspend fun initiatePayment(@Body body: Map<String, Any>): Response<ApiResponse<Any>>

    @POST("api/payments/stripe/intent")
    suspend fun createStripeIntent(@Body body: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @POST("api/payments/stripe/confirm")
    suspend fun confirmStripePayment(@Body body: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @PATCH("api/expenses/settle/{splitId}")
    suspend fun settleSplit(@Path("splitId") splitId: Long): Response<ApiResponse<Any>>

    // ==================== USER PROFILE ENDPOINTS ====================

    @GET("api/users/me")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    @PATCH("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserProfile>>

    @POST("api/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<String>>

    // ==================== NOTIFICATION ENDPOINTS ====================

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<Notification>>>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<ApiResponse<Int>>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<ApiResponse<Any>>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Any>>

    // ==================== ADMIN ENDPOINTS ====================

    @GET("api/admin/users")
    suspend fun getAllUsers(): Response<ApiResponse<List<Map<String, Any>>>>

    @PATCH("api/admin/users/{userId}/deactivate")
    suspend fun deactivateUser(@Path("userId") userId: Long): Response<ApiResponse<Any>>

    @PATCH("api/admin/users/{userId}/reactivate")
    suspend fun reactivateUser(@Path("userId") userId: Long): Response<ApiResponse<Any>>

    @GET("api/admin/groups")
    suspend fun getAllGroups(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("api/admin/system/stats")
    suspend fun getSystemStats(): Response<ApiResponse<Map<String, Any>>>

    @GET("api/admin/system/logs")
    suspend fun getSystemLogs(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<Map<String, Any>>>>
}