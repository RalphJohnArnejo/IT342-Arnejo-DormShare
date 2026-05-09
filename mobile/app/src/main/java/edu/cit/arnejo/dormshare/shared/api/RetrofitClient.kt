package edu.cit.arnejo.dormshare.shared.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the DormShare backend.
 *
 * For Android Emulator:  use "http://10.0.2.2:8080/" (maps to host machine's localhost)
 * For Physical Device:   use your computer's local IP, e.g., "http://192.168.1.100:8080/"
 * For Deployed Backend:  use the deployed URL, e.g., "https://your-backend.com/"
 */
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.shared.auth.TokenProvider

object RetrofitClient {

    // Change this URL based on your setup
    private const val BASE_URL = "http://10.0.2.2:8080/"

    /** Application context – set once from Application.onCreate() or LoginActivity */
    private var appContext: Context? = null

    /**
     * Must be called once (e.g. from Application.onCreate or LoginActivity) so the
     * auth interceptor can fall back to reading the persisted token from SharedPreferences.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val builder = original.newBuilder()

        // Try in-memory token first, then fall back to persisted session token
        val token = TokenProvider.token
            ?: appContext?.let { ctx ->
                SessionManager.getToken(ctx)?.also { saved ->
                    // Re-hydrate the in-memory holder so subsequent calls are fast
                    TokenProvider.token = saved
                }
            }

        token?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(builder.build())
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
