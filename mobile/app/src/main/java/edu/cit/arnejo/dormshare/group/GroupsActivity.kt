package edu.cit.arnejo.dormshare.group

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import edu.cit.arnejo.dormshare.group.GroupAdapter
import edu.cit.arnejo.dormshare.shared.api.RetrofitClient
import edu.cit.arnejo.dormshare.shared.auth.SessionManager
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.databinding.ActivityGroupsBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsBinding
    private lateinit var adapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GroupAdapter(
            emptyList(),
            onItemClick = { group ->
                SessionManager.setSelectedGroupId(this, group.id)
                Toast.makeText(this, "Selected: ${group.name}", Toast.LENGTH_SHORT).show()
            },
            onLeaveGroup = { group ->
                confirmLeaveGroup(group)
            }
        )
        binding.rvGroups.layoutManager = LinearLayoutManager(this)
        binding.rvGroups.adapter = adapter

        binding.btnCreateGroup.setOnClickListener { showCreateGroupDialog() }
        binding.btnJoinGroup.setOnClickListener { showJoinGroupDialog() }

        loadGroups()
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups()
                if (response.isSuccessful) {
                    val list = response.body()?.data ?: emptyList()
                    adapter.update(list)
                    binding.tvEmptyGroups.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    Toast.makeText(this@GroupsActivity, "Failed to load groups", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateGroupDialog() {
        val input = TextInputEditText(this)
        input.hint = "e.g. Room 301, Apartment 4B..."
        input.setPadding(40, 40, 40, 40)

        AlertDialog.Builder(this)
            .setTitle("Create Group")
            .setMessage("Enter a name for your new dorm group")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createGroup(name)
                } else {
                    Toast.makeText(this, "Group name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createGroup(name: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createGroup(mapOf("name" to name))
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupsActivity, "Group \"$name\" created!", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    val msg = response.body()?.error?.message ?: "Failed to create group"
                    Toast.makeText(this@GroupsActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showJoinGroupDialog() {
        val input = TextInputEditText(this)
        input.hint = "Enter invite code (e.g. 94F6F8)"
        input.setPadding(40, 40, 40, 40)

        AlertDialog.Builder(this)
            .setTitle("Join Group")
            .setMessage("Enter the invite code you received")
            .setView(input)
            .setPositiveButton("Join") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) {
                    joinGroup(code)
                } else {
                    Toast.makeText(this, "Invite code is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinGroup(code: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.joinGroup(mapOf("inviteCode" to code))
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupsActivity, "Joined group!", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this@GroupsActivity, "Invalid code", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmLeaveGroup(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave \"${group.name}\"?")
            .setPositiveButton("Leave") { _, _ ->
                leaveGroup(group.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup(groupId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.leaveGroup(groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupsActivity, "Left group", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this@GroupsActivity, "Failed to leave group", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}