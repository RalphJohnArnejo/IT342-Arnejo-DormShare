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
import java.util.stream.Collectors;

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

        // Find group by invite code
        Optional<GroupEntity> groupOpt = groupRepository.findByInviteCode(inviteCode.trim().toUpperCase());
        if (groupOpt.isEmpty()) {
            return ApiResponse.error("GROUP-002", "Invalid invite code",
                    "No group found with that invite code");
        }

        GroupEntity group = groupOpt.get();

        // Check if user is already in this specific group
        if (membershipRepository.existsByUserIdAndGroupId(userId, group.getId())) {
            return ApiResponse.error("GROUP-001", "Already a member",
                    "You are already a member of this group");
        }

        // Add user as MEMBER
        GroupMembershipEntity membership = new GroupMembershipEntity();
        membership.setGroupId(group.getId());
        membership.setUserId(userId);
        membership.setRole("MEMBER");
        membershipRepository.save(membership);

        return ApiResponse.ok(mapToResponse(group));
    }

    /**
     * Leave a specific group.
     * If the user is the ADMIN and others remain, promote the next member.
     * If the user is the last member, delete the group.
     */
    @Transactional
    public ApiResponse leaveGroup(Long groupId, Long userId) {
        Optional<GroupMembershipEntity> membershipOpt = membershipRepository.findByUserIdAndGroupId(userId, groupId);
        if (membershipOpt.isEmpty()) {
            return ApiResponse.error("GROUP-003", "Not in this group", "You are not a member of this group");
        }

        GroupMembershipEntity membership = membershipOpt.get();
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
     * Get all groups the user belongs to.
     */
    public ApiResponse getMyGroups(Long userId) {
        List<GroupMembershipEntity> memberships = membershipRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return ApiResponse.ok(new ArrayList<>());
        }

        List<GroupResponse> groups = memberships.stream()
                .map(m -> groupRepository.findById(m.getGroupId()).orElse(null))
                .filter(g -> g != null)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.ok(groups);
    }

    /**
     * Get a specific group's details.
     */
    public ApiResponse getGroupById(Long groupId, Long userId) {
        // Verify user is a member
        if (!membershipRepository.existsByUserIdAndGroupId(userId, groupId)) {
            return ApiResponse.error("GROUP-003", "Not a member", "You are not a member of this group");
        }

        Optional<GroupEntity> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ApiResponse.error("GROUP-004", "Group not found", "Group no longer exists");
        }

        return ApiResponse.ok(mapToResponse(groupOpt.get()));
    }

    /**
     * Get the group ID for a user. For controllers that need a single active group.
     * If user is in multiple groups, returns the first one.
     * Returns null if user is not in any group.
     */
    public Long getUserGroupId(Long userId) {
        List<GroupMembershipEntity> memberships = membershipRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return null;
        }
        return memberships.get(0).getGroupId();
    }

    /**
     * Get a specific group ID for a user, verifying membership.
     * Returns null if user is not a member of the given group.
     */
    public Long getVerifiedGroupId(Long userId, Long groupId) {
        if (groupId == null) return getUserGroupId(userId);
        if (membershipRepository.existsByUserIdAndGroupId(userId, groupId)) {
            return groupId;
        }
        return null;
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
