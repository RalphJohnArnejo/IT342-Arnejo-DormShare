package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.GroupResponse;
import edu.cit.arnejo.dormshare.entity.GroupEntity;
import edu.cit.arnejo.dormshare.entity.GroupMembershipEntity;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.repository.GroupRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository membershipRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new group. The creator becomes the ADMIN member.
     */
    @Transactional
    public ApiResponse createGroup(String name, Long userId) {
        if (name == null || name.trim().isEmpty()) {
            return ApiResponse.error("VALID-001", "Validation failed", "Group name is required");
        }

        // Check if user is already in a group
        if (membershipRepository.existsByUserId(userId)) {
            return ApiResponse.error("GROUP-001", "Already in a group",
                    "You must leave your current group before creating a new one");
        }

        // Create group
        GroupEntity group = new GroupEntity();
        group.setName(name.trim());
        group.setCreatedById(userId);
        group = groupRepository.save(group);

        // Add creator as ADMIN member
        GroupMembershipEntity membership = new GroupMembershipEntity();
        membership.setGroupId(group.getId());
        membership.setUserId(userId);
        membership.setRole("ADMIN");
        membershipRepository.save(membership);

        return ApiResponse.ok(mapToResponse(group));
    }

    /**
     * Join a group using an invite code.
     */
    @Transactional
    public ApiResponse joinGroup(String inviteCode, Long userId) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return ApiResponse.error("VALID-001", "Validation failed", "Invite code is required");
        }

        // Check if user is already in a group
        if (membershipRepository.existsByUserId(userId)) {
            return ApiResponse.error("GROUP-001", "Already in a group",
                    "You must leave your current group before joining another");
        }

        // Find group by invite code
        Optional<GroupEntity> groupOpt = groupRepository.findByInviteCode(inviteCode.trim().toUpperCase());
        if (groupOpt.isEmpty()) {
            return ApiResponse.error("GROUP-002", "Invalid invite code",
                    "No group found with that invite code");
        }

        GroupEntity group = groupOpt.get();

        // Add user as MEMBER
        GroupMembershipEntity membership = new GroupMembershipEntity();
        membership.setGroupId(group.getId());
        membership.setUserId(userId);
        membership.setRole("MEMBER");
        membershipRepository.save(membership);

        return ApiResponse.ok(mapToResponse(group));
    }

    /**
     * Leave the current group.
     * If the user is the ADMIN and others remain, promote the next member.
     * If the user is the last member, delete the group.
     */
    @Transactional
    public ApiResponse leaveGroup(Long userId) {
        Optional<GroupMembershipEntity> membershipOpt = membershipRepository.findByUserId(userId);
        if (membershipOpt.isEmpty()) {
            return ApiResponse.error("GROUP-003", "Not in a group", "You are not in any group");
        }

        GroupMembershipEntity membership = membershipOpt.get();
        Long groupId = membership.getGroupId();
        boolean wasAdmin = "ADMIN".equals(membership.getRole());

        // Remove the user
        membershipRepository.deleteByUserIdAndGroupId(userId, groupId);

        // Check remaining members
        long remaining = membershipRepository.countByGroupId(groupId);

        if (remaining == 0) {
            // Last member left — delete the group
            groupRepository.deleteById(groupId);
        } else if (wasAdmin) {
            // Promote the first remaining member to ADMIN
            List<GroupMembershipEntity> members = membershipRepository.findByGroupId(groupId);
            if (!members.isEmpty()) {
                GroupMembershipEntity newAdmin = members.get(0);
                newAdmin.setRole("ADMIN");
                membershipRepository.save(newAdmin);
            }
        }

        return ApiResponse.ok("Successfully left the group");
    }

    /**
     * Get the current user's group with all members.
     */
    public ApiResponse getMyGroup(Long userId) {
        Optional<GroupMembershipEntity> membershipOpt = membershipRepository.findByUserId(userId);
        if (membershipOpt.isEmpty()) {
            return ApiResponse.ok(null); // No group — frontend handles this
        }

        Long groupId = membershipOpt.get().getGroupId();
        Optional<GroupEntity> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ApiResponse.error("GROUP-004", "Group not found", "Your group no longer exists");
        }

        return ApiResponse.ok(mapToResponse(groupOpt.get()));
    }

    /**
     * Get the group ID for a user. Returns null if user is not in any group.
     * Used internally by other services (Pantry, Expenses).
     */
    public Long getUserGroupId(Long userId) {
        return membershipRepository.findByUserId(userId)
                .map(GroupMembershipEntity::getGroupId)
                .orElse(null);
    }

    /**
     * Map a GroupEntity to a GroupResponse with member details.
     */
    private GroupResponse mapToResponse(GroupEntity group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setInviteCode(group.getInviteCode());
        response.setCreatedAt(group.getCreatedAt());

        // Build member list
        List<GroupMembershipEntity> memberships = membershipRepository.findByGroupId(group.getId());
        List<GroupResponse.MemberInfo> members = new ArrayList<>();

        for (GroupMembershipEntity m : memberships) {
            GroupResponse.MemberInfo info = new GroupResponse.MemberInfo();
            info.setUserId(m.getUserId());
            info.setRole(m.getRole());
            info.setJoinedAt(m.getJoinedAt());

            userRepository.findById(m.getUserId()).ifPresent(user -> {
                info.setName(user.getFirstName() + " " + user.getLastName());
                info.setEmail(user.getEmail());
            });

            members.add(info);
        }

        response.setMembers(members);
        return response;
    }
}
