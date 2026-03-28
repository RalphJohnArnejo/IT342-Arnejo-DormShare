package edu.cit.arnejo.dormshare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.api.RetrofitClient
import edu.cit.arnejo.dormshare.model.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        // Register button click
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }

        // Navigate to Login screen
        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (firstName.isEmpty()) {
            etFirstName.error = getString(R.string.error_empty_field)
            etFirstName.requestFocus()
            return false
        }

        if (lastName.isEmpty()) {
            etLastName.error = getString(R.string.error_empty_field)
            etLastName.requestFocus()
            return false
        }

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

        if (password.length < 8) {
            etPassword.error = getString(R.string.error_short_password)
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performRegistration() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Show loading state
        setLoading(true)
        tvError.visibility = View.GONE

        val request = RegisterRequest(firstName, lastName, email, password)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Show success dialog
                    showSuccessDialog()
                } else {
                    // Show error from backend
                    val errorMessage = response.body()?.error?.message
                        ?: response.body()?.error?.details?.toString()
                        ?: "Registration failed. Please try again."
                    showError(errorMessage)
                }
            } catch (e: Exception) {
                showError(getString(R.string.error_network) + "\n" + e.localizedMessage)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.registration_successful))
            .setMessage(getString(R.string.registration_successful_message))
            .setPositiveButton("Go to Login") { dialog, _ ->
                dialog.dismiss()
                // Navigate back to Login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !loading
        etFirstName.isEnabled = !loading
        etLastName.isEnabled = !loading
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}
