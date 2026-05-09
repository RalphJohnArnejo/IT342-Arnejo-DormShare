package edu.cit.arnejo.dormshare.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.arnejo.dormshare.R
import edu.cit.arnejo.dormshare.databinding.ItemGroupBinding
import edu.cit.arnejo.dormshare.databinding.ItemMemberBinding
import edu.cit.arnejo.dormshare.group.Group
import edu.cit.arnejo.dormshare.group.GroupMember

class GroupAdapter(
    private var groups: List<Group>,
    private val onItemClick: ((Group) -> Unit)? = null,
    private val onLeaveGroup: ((Group) -> Unit)? = null
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        val binding = holder.binding

        binding.tvGroupName.text = group.name
        binding.tvAdminBadge.visibility = if (group.isAdmin) View.VISIBLE else View.GONE
        binding.tvInviteCode.text = group.inviteCode?.chunked(1)?.joinToString(" ") ?: "N/A"
        binding.tvCreatedDate.text = group.createdAt?.let { "Created $it" } ?: ""

        // Members RecyclerView
        binding.rvMembers.layoutManager = LinearLayoutManager(binding.root.context)
        val membersAdapter = MemberAdapter(group.members ?: emptyList())
        binding.rvMembers.adapter = membersAdapter

        // Leave button
        binding.btnLeaveGroup.setOnClickListener {
            onLeaveGroup?.invoke(group)
        }

        // Copy invite code
        binding.btnCopyCode.setOnClickListener {
            val clipboard = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val code = group.inviteCode ?: ""
            val clip = ClipData.newPlainText("Invite Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(binding.root.context, "Code copied!", Toast.LENGTH_SHORT).show()
        }

        // Click to select
        binding.root.setOnClickListener {
            onItemClick?.invoke(group)
        }
    }

    override fun getItemCount() = groups.size

    fun update(newGroups: List<Group>) {
        groups = newGroups
        notifyDataSetChanged()
    }
}

class MemberAdapter(
    private val members: List<GroupMember>
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        val binding = holder.binding

        binding.tvMemberName.text = member.name
        binding.tvMemberEmail.text = member.email
        binding.tvMemberInitial.text = member.name.firstOrNull()?.uppercase() ?: "?"
        binding.tvAdminCrown.visibility = if (member.isAdmin) View.VISIBLE else View.GONE
        binding.tvAdminCrown.text = "👑"
        binding.tvYouBadge.visibility = if (member.isCurrentUser) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = members.size
}
