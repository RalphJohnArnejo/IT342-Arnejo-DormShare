package edu.cit.arnejo.dormshare.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.home.HomeActivity
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.shared.auth.TokenProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvRegisterLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize RetrofitClient with app context so auth token can be read from SharedPreferences
        RetrofitClient.init(this)

        // Check for existing session (auto-login)
        lifecycleScope.launch {
            val savedToken = SessionManager.getToken(this@LoginActivity)
            if (!savedToken.isNullOrEmpty()) {
                TokenProvider.token = savedToken

                // Verify the token is still valid by making a quick API call
                try {
                    val testResponse = RetrofitClient.apiService.getGroups()
                    if (testResponse.code() == 401) {
                        // Token expired or invalid – clear session, show login
                        SessionManager.clearSession(this@LoginActivity)
                        TokenProvider.token = null
                        Log.d("AUTH_DEBUG", "Saved token expired, showing login")
                        return@launch
                    }
                } catch (_: Exception) {
                    // Network error – still try auto-login (may work offline later)
                }

                val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                    putExtra("firstName", SessionManager.getFirstName(this@LoginActivity) ?: "")
                    putExtra("lastName", SessionManager.getLastName(this@LoginActivity) ?: "")
                    putExtra("email", SessionManager.getEmail(this@LoginActivity) ?: "")
                    putExtra("role", SessionManager.getRole(this@LoginActivity) ?: "USER")
                    putExtra("token", savedToken)
                }
                startActivity(intent)
                finish()
            }
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        findViewById<MaterialButton>(R.id.btnGoogleSignIn).setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = getString(R.string.error_empty_field)
            etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.error_invalid_email)
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_empty_field)
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        setLoading(true)
        tvError.visibility = View.GONE

        val request = LoginRequest(email, password)

        lifecycleScope.launch {
            try {
                // Inside performLogin() in LoginActivity.kt
                val response = RetrofitClient.apiService.login(request)
                val apiResponse = response.body()

                if (response.isSuccessful && apiResponse?.success == true) {
                    // Get the typed AuthResponse object (flat structure from backend)
                    val authData = apiResponse.data!!

                    // Access properties directly from the flat structure
                    val token = authData.token
                    val firstName = authData.firstName
                    val lastName = authData.lastName
                    val userEmail = authData.email
                    val role = authData.role
                    val userId = authData.userId

                    // Save the token to TokenProvider so other screens can access data
                    TokenProvider.token = token
                    Log.d("AUTH_DEBUG", "Token saved: $token")

                    SessionManager.saveSession(this@LoginActivity, token, userId, userEmail, firstName, lastName, role)

                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    // 4. Correctly extract the error message from your new ApiError model
                    val errorMessage = apiResponse?.error?.message ?: getString(R.string.invalid_credentials)
                    showError(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("AUTH_ERROR", "Login failed", e)
                showError(getString(R.string.error_network) + "\n" + e.localizedMessage)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}