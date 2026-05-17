package edu.cit.arnejo.dormshare.group

data class Group(
    val id: Long,
    val name: String,
    val description: String? = null,
    val inviteCode: String? = null,
    val createdAt: String? = null,
    val members: List<GroupMember>? = null,
    val isAdmin: Boolean = false
)

data class GroupMember(
    val id: Long = 0,
    val userId: Long = 0,
    val name: String,
    val email: String,
    val role: String? = null,
    val isAdmin: Boolean = false,
    val isCurrentUser: Boolean = false
)
