package edu.cit.arnejo.dormshare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnExpenses: MaterialButton
    private lateinit var btnGroups: MaterialButton
    private lateinit var btnSettlements: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        btnLogout = findViewById(R.id.btnLogout)
        btnExpenses = findViewById(R.id.btnExpenses)
        btnGroups = findViewById(R.id.btnGroups)
        btnSettlements = findViewById(R.id.btnSettlements)

        // Get user data from intent
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val role = intent.getStringExtra("role") ?: "USER"

        // Display user info
        tvUserName.text = "$firstName $lastName"
        tvUserEmail.text = "📧  $email"
        tvUserRole.text = "Role: $role"

        // Logout button
        btnLogout.setOnClickListener {
            // Navigate back to Login and clear the back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnExpenses.setOnClickListener {
            val intent = Intent(this, ExpensesActivity::class.java)
            startActivity(intent)
        }

        btnGroups.setOnClickListener {
            val intent = Intent(this, GroupsActivity::class.java)
            startActivity(intent)
        }

        btnSettlements.setOnClickListener {
            val intent = Intent(this, SettlementsActivity::class.java)
            startActivity(intent)
        }
    }
}
