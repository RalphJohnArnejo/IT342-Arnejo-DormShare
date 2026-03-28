package edu.cit.arnejo.dormshare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.model.LoginRequest
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

        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        // Login button click
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        // Navigate to Register screen
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

        // Show loading state
        setLoading(true)
        tvError.visibility = View.GONE

        val request = LoginRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val firstName = data?.get("firstName")?.toString() ?: ""
                    val lastName = data?.get("lastName")?.toString() ?: ""
                    val userEmail = data?.get("email")?.toString() ?: email
                    val role = data?.get("role")?.toString() ?: "USER"
                    val token = data?.get("token")?.toString() ?: ""

                    Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                    // Navigate to Home screen with user data
                    val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                        putExtra("firstName", firstName)
                        putExtra("lastName", lastName)
                        putExtra("email", userEmail)
                        putExtra("role", role)
                        putExtra("token", token)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Show error from backend
                    val errorMessage = response.body()?.error?.message
                        ?: response.body()?.error?.details?.toString()
                        ?: getString(R.string.invalid_credentials)
                    showError(errorMessage)
                }
            } catch (e: Exception) {
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
