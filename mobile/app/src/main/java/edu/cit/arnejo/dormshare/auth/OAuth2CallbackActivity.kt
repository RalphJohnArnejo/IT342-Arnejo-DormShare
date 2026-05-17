package edu.cit.arnejo.dormshare.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.cit.arnejo.dormshare.home.HomeActivity
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.shared.auth.TokenProvider

/**
 * Handles the dormshare://oauth2/callback deep link after Google OAuth2 completes.
 *
 * Flow:
 * 1. LoginActivity opens Chrome Custom Tab → /auth/google-mobile
 * 2. Backend sets session flag, redirects to /oauth2/authorization/google
 * 3. Google auth happens in browser
 * 4. Backend OAuth2LoginSuccessHandler detects mobile flag,
 *    redirects to dormshare://oauth2/callback?token=...&userId=...&email=...
 * 5. Android intercepts the deep link, this Activity receives the Intent
 * 6. We extract the auth params, save session, and launch HomeActivity
 */
class OAuth2CallbackActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OAuth2Callback"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri: Uri? = intent.data
        Log.d(TAG, "Received URI: $uri")

        if (uri == null || uri.scheme != "dormshare") {
            Toast.makeText(this, "Invalid OAuth2 callback", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val token = uri.getQueryParameter("token")
        val userId = uri.getQueryParameter("userId")?.toLongOrNull()
        val email = uri.getQueryParameter("email")
        val firstName = uri.getQueryParameter("firstName")
        val lastName = uri.getQueryParameter("lastName")
        val role = uri.getQueryParameter("role")

        if (token.isNullOrEmpty() || userId == null || email.isNullOrEmpty()) {
            Log.e(TAG, "Missing required params: token=$token, userId=$userId, email=$email")
            Toast.makeText(this, "Google Sign-In failed — missing data", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Save session exactly like the standard login flow
        TokenProvider.token = token
        SessionManager.saveSession(
            this,
            token = token,
            userId = userId,
            email = email,
            firstName = firstName ?: "",
            lastName = lastName ?: "",
            role = role ?: "USER"
        )

        Log.d(TAG, "Google Sign-In successful for $email (userId=$userId)")
        Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show()

        // Navigate to HomeActivity and clear the back stack
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
