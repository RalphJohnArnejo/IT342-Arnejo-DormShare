package edu.cit.arnejo.dormshare.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.auth.LoginActivity
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.settlement.SettlementsActivity
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.shared.auth.TokenProvider
import kotlinx.coroutines.launch

/**
 * Profile screen — mirrors the web Profile.jsx.
 * Supports: view profile, edit profile, change password, logout.
 */
class ProfileActivity : AppCompatActivity() {

    // Hero
    private lateinit var tvInitials: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var tvMemberSince: TextView

    // Account Details — view mode
    private lateinit var layoutViewMode: LinearLayout
    private lateinit var btnEditProfile: MaterialButton

    // Account Details — edit mode
    private lateinit var layoutEditMode: LinearLayout
    private lateinit var etEditFirstName: TextInputEditText
    private lateinit var etEditLastName: TextInputEditText
    private lateinit var etEditEmail: TextInputEditText
    private lateinit var btnCancelEdit: MaterialButton
    private lateinit var btnSaveProfile: MaterialButton

    // Security — view
    private lateinit var layoutPasswordView: LinearLayout
    private lateinit var btnChangePassword: MaterialButton

    // Security — form
    private lateinit var layoutPasswordForm: LinearLayout
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnCancelPassword: MaterialButton
    private lateinit var btnSubmitPassword: MaterialButton

    private lateinit var btnLogout: MaterialButton
    private lateinit var progressBar: ProgressBar

    // My Groups
    private lateinit var layoutGroupsList: LinearLayout
    private lateinit var tvGroupCount: TextView
    private lateinit var tvNoGroups: TextView

    // Settlement
    private lateinit var cardSettlement: CardView

    private var currentProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupClickListeners()
        loadProfile()
        loadGroups()
    }

    private fun initViews() {
        tvInitials = findViewById(R.id.tvInitials)
        tvFullName = findViewById(R.id.tvFullName)
        tvEmail = findViewById(R.id.tvEmail)
        tvRoleBadge = findViewById(R.id.tvRoleBadge)
        tvMemberSince = findViewById(R.id.tvMemberSince)

        layoutViewMode = findViewById(R.id.layoutViewMode)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        layoutEditMode = findViewById(R.id.layoutEditMode)
        etEditFirstName = findViewById(R.id.etEditFirstName)
        etEditLastName = findViewById(R.id.etEditLastName)
        etEditEmail = findViewById(R.id.etEditEmail)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        layoutPasswordView = findViewById(R.id.layoutPasswordView)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        layoutPasswordForm = findViewById(R.id.layoutPasswordForm)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCancelPassword = findViewById(R.id.btnCancelPassword)
        btnSubmitPassword = findViewById(R.id.btnSubmitPassword)

        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)

        // My Groups
        layoutGroupsList = findViewById(R.id.layoutGroupsList)
        tvGroupCount = findViewById(R.id.tvGroupCount)
        tvNoGroups = findViewById(R.id.tvNoGroups)

        // Settlement
        cardSettlement = findViewById(R.id.cardSettlement)
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener { enterEditMode() }
        btnCancelEdit.setOnClickListener { exitEditMode() }
        btnSaveProfile.setOnClickListener { saveProfile() }

        btnChangePassword.setOnClickListener {
            layoutPasswordView.visibility = View.GONE
            layoutPasswordForm.visibility = View.VISIBLE
        }
        btnCancelPassword.setOnClickListener {
            layoutPasswordForm.visibility = View.GONE
            layoutPasswordView.visibility = View.VISIBLE
            clearPasswordFields()
        }
        btnSubmitPassword.setOnClickListener { submitPasswordChange() }

        btnLogout.setOnClickListener { logout() }

        cardSettlement.setOnClickListener {
            startActivity(Intent(this, SettlementsActivity::class.java))
        }
    }

    private fun loadProfile() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getProfile()
                if (response.isSuccessful && response.body()?.success == true) {
                    currentProfile = response.body()?.data
                    displayProfile(currentProfile!!)
                } else {
                    showToast("Failed to load profile")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.localizedMessage}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayProfile(profile: UserProfile) {
        val first = profile.firstName ?: ""
        val last = profile.lastName ?: ""
        val initials = "${first.firstOrNull()?.uppercase() ?: ""}${last.firstOrNull()?.uppercase() ?: ""}"

        tvInitials.text = initials.ifEmpty { "?" }
        tvFullName.text = "$first $last".trim().ifEmpty { "User" }
        tvEmail.text = profile.email ?: ""
        tvRoleBadge.text = profile.role ?: "USER"
        tvMemberSince.text = profile.createdAt?.let {
            try {
                "Member since ${it.substringBefore("T")}"
            } catch (_: Exception) { "Member" }
        } ?: "Member"

        // Set detail rows
        setDetailRow(R.id.rowFirstName, "First Name", first)
        setDetailRow(R.id.rowLastName, "Last Name", last)
        setDetailRow(R.id.rowEmail, "Email", profile.email ?: "")
        setDetailRow(R.id.rowRole, "Role", profile.role ?: "USER")
    }

    private fun setDetailRow(viewId: Int, label: String, value: String) {
        val row = findViewById<View>(viewId) ?: return
        row.findViewById<TextView>(R.id.tvLabel)?.text = label
        row.findViewById<TextView>(R.id.tvValue)?.text = value
    }

    /**
     * Load the user's groups and display them in the My Groups card.
     * Matches the web Profile.jsx "My Groups" section.
     */
    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups()
                if (response.isSuccessful) {
                    val groups = response.body()?.data ?: emptyList()
                    displayGroups(groups)
                }
            } catch (_: Exception) {
                // Silently fail — groups is a nice-to-have on the profile screen
            }
        }
    }

    private fun displayGroups(groups: List<Group>) {
        tvGroupCount.text = groups.size.toString()
        layoutGroupsList.removeAllViews()

        if (groups.isEmpty()) {
            tvNoGroups.visibility = View.VISIBLE
            return
        }
        tvNoGroups.visibility = View.GONE

        for (group in groups) {
            val row = layoutInflater.inflate(R.layout.item_group_profile, layoutGroupsList, false)
            row.findViewById<TextView>(R.id.tvGroupName).text = group.name ?: "Group"
            val memberCount = group.members?.size ?: 0
            row.findViewById<TextView>(R.id.tvMemberCount).text = "$memberCount member${if (memberCount != 1) "s" else ""}"
            layoutGroupsList.addView(row)
        }
    }

    private fun enterEditMode() {
        currentProfile?.let {
            etEditFirstName.setText(it.firstName ?: "")
            etEditLastName.setText(it.lastName ?: "")
            etEditEmail.setText(it.email ?: "")
        }
        layoutViewMode.visibility = View.GONE
        layoutEditMode.visibility = View.VISIBLE
        btnEditProfile.visibility = View.GONE
    }

    private fun exitEditMode() {
        layoutEditMode.visibility = View.GONE
        layoutViewMode.visibility = View.VISIBLE
        btnEditProfile.visibility = View.VISIBLE
    }

    private fun saveProfile() {
        val firstName = etEditFirstName.text.toString().trim()
        val lastName = etEditLastName.text.toString().trim()
        val email = etEditEmail.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showToast("All fields are required")
            return
        }

        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        lifecycleScope.launch {
            try {
                val request = UpdateProfileRequest(firstName, lastName, email)
                val response = RetrofitClient.apiService.updateProfile(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val updated = response.body()?.data!!
                    currentProfile = updated
                    displayProfile(updated)
                    exitEditMode()

                    // Update session so other screens reflect changes
                    SessionManager.saveSession(
                        this@ProfileActivity,
                        SessionManager.getToken(this@ProfileActivity) ?: "",
                        updated.id,
                        updated.email ?: "",
                        updated.firstName ?: "",
                        updated.lastName ?: "",
                        updated.role ?: "USER"
                    )
                    showToast("Profile updated!")
                } else {
                    showToast("Failed to update profile")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            } finally {
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Changes"
            }
        }
    }

    private fun submitPasswordChange() {
        val current = etCurrentPassword.text.toString()
        val newPw = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()

        if (current.isEmpty() || newPw.isEmpty()) {
            showToast("All password fields are required")
            return
        }
        if (newPw.length < 8) {
            showToast("New password must be at least 8 characters")
            return
        }
        if (newPw != confirm) {
            showToast("New passwords do not match")
            return
        }

        btnSubmitPassword.isEnabled = false
        btnSubmitPassword.text = "Changing..."

        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(current, newPw)
                val response = RetrofitClient.apiService.changePassword(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("Password changed successfully!")
                    layoutPasswordForm.visibility = View.GONE
                    layoutPasswordView.visibility = View.VISIBLE
                    clearPasswordFields()
                } else {
                    val msg = response.body()?.error?.message ?: "Failed to change password"
                    showToast(msg)
                }
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            } finally {
                btnSubmitPassword.isEnabled = true
                btnSubmitPassword.text = "Update Password"
            }
        }
    }

    private fun clearPasswordFields() {
        etCurrentPassword.text?.clear()
        etNewPassword.text?.clear()
        etConfirmPassword.text?.clear()
    }

    private fun logout() {
        SessionManager.clearSession(this)
        TokenProvider.token = null
        showToast("Logged out successfully")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
