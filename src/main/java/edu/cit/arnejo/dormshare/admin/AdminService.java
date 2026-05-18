package edu.cit.arnejo.dormshare.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.admin.dto.UserManagementDTO;
import edu.cit.arnejo.dormshare.admin.entity.AuditLogEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.expense.ExpenseRepository;
import edu.cit.arnejo.dormshare.group.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.group.GroupRepository;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                       GroupRepository groupRepository,
                       GroupMembershipRepository groupMembershipRepository,
                       ExpenseRepository expenseRepository,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.expenseRepository = expenseRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get all users with their details
     */
    public ApiResponse getAllUsers() {
        try {
            List<UserManagementDTO> users = userRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(user -> new UserManagementDTO(
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole(),
                            user.getIsActive(),
                            user.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            return ApiResponse.ok(users);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-001", "Failed to fetch users", e.getMessage());
        }
    }

    /**
     * Deactivate a user — sets isActive to false in the database
     */
    @Transactional
    public ApiResponse deactivateUser(Long userId, UserEntity admin) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            // Prevent deactivating yourself
            if (user.getId().equals(admin.getId())) {
                return ApiResponse.error("ADMIN-010", "Cannot deactivate self", "You cannot deactivate your own account");
            }

            // Prevent deactivating other admins
            if ("ADMIN".equals(user.getRole())) {
                return ApiResponse.error("ADMIN-011", "Cannot deactivate admin", "Cannot deactivate another admin account");
            }

            if (!user.getIsActive()) {
                return ApiResponse.error("ADMIN-012", "Already inactive", "User is already deactivated");
            }

            user.setIsActive(false);
            userRepository.save(user);

            // Log the action
            logAuditEvent("USER_DEACTIVATED", admin, "USER", userId,
                    "Deactivated user: " + user.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("isActive", false);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-002", "Failed to deactivate user", e.getMessage());
        }
    }

    /**
     * Reactivate a user — sets isActive to true in the database
     */
    @Transactional
    public ApiResponse reactivateUser(Long userId, UserEntity admin) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            if (user.getIsActive()) {
                return ApiResponse.error("ADMIN-013", "Already active", "User is already active");
            }

            user.setIsActive(true);
            userRepository.save(user);

            // Log the action
            logAuditEvent("USER_REACTIVATED", admin, "USER", userId,
                    "Reactivated user: " + user.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("isActive", true);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-003", "Failed to reactivate user", e.getMessage());
        }
    }

    /**
     * Promote a user to ADMIN role
     */
    @Transactional
    public ApiResponse promoteToAdmin(Long userId, UserEntity admin) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            if ("ADMIN".equals(user.getRole())) {
                return ApiResponse.error("ADMIN-014", "Already admin", "User already has ADMIN role");
            }

            user.setRole("ADMIN");
            userRepository.save(user);

            logAuditEvent("USER_PROMOTED", admin, "USER", userId,
                    "Promoted user to ADMIN: " + user.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("role", "ADMIN");
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-015", "Failed to promote user", e.getMessage());
        }
    }

    /**
     * Demote an admin to USER role
     */
    @Transactional
    public ApiResponse demoteToUser(Long userId, UserEntity admin) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            if (user.getId().equals(admin.getId())) {
                return ApiResponse.error("ADMIN-016", "Cannot demote self", "You cannot demote your own account");
            }

            if (!"ADMIN".equals(user.getRole())) {
                return ApiResponse.error("ADMIN-017", "Not an admin", "User does not have ADMIN role");
            }

            user.setRole("USER");
            userRepository.save(user);

            logAuditEvent("USER_DEMOTED", admin, "USER", userId,
                    "Demoted user from ADMIN: " + user.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("role", "USER");
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-018", "Failed to demote user", e.getMessage());
        }
    }

    /**
     * Get all groups with member counts and creation dates
     */
    public ApiResponse getAllGroups() {
        try {
            List<Map<String, Object>> groups = groupRepository.findAll().stream()
                    .map(group -> {
                        Map<String, Object> groupData = new HashMap<>();
                        groupData.put("id", group.getId());
                        groupData.put("name", group.getName());
                        groupData.put("inviteCode", group.getInviteCode());
                        groupData.put("createdAt", group.getCreatedAt());
                        groupData.put("createdById", group.getCreatedById());
                        groupData.put("memberCount", groupMembershipRepository.countByGroupId(group.getId()));
                        return groupData;
                    })
                    .collect(Collectors.toList());

            return ApiResponse.ok(groups);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-004", "Failed to fetch groups", e.getMessage());
        }
    }

    /**
     * Get system activity logs from the audit_logs table
     */
    public ApiResponse getSystemLogs(int limit, int offset) {
        try {
            List<Map<String, Object>> logs = auditLogRepository
                    .findAllByOrderByCreatedAtDesc(PageRequest.of(offset / Math.max(limit, 1), Math.max(limit, 1)))
                    .stream()
                    .map(log -> {
                        Map<String, Object> logData = new HashMap<>();
                        logData.put("id", log.getId());
                        logData.put("action", log.getAction());
                        logData.put("user", log.getPerformedByEmail());
                        logData.put("targetType", log.getTargetType());
                        logData.put("targetId", log.getTargetId());
                        logData.put("details", log.getDetails());
                        logData.put("timestamp", log.getCreatedAt());
                        return logData;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("logs", logs);
            data.put("total", auditLogRepository.count());
            data.put("limit", limit);
            data.put("offset", offset);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-005", "Failed to fetch logs", e.getMessage());
        }
    }

    /**
     * Get real system statistics
     */
    public ApiResponse getSystemStats() {
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActive(true);
            long inactiveUsers = userRepository.countByIsActive(false);
            long totalGroups = groupRepository.count();
            long totalExpenses = expenseRepository.count();
            long totalAuditLogs = auditLogRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("inactiveUsers", inactiveUsers);
            stats.put("totalGroups", totalGroups);
            stats.put("totalExpenses", totalExpenses);
            stats.put("totalAuditLogs", totalAuditLogs);
            stats.put("timestamp", System.currentTimeMillis());

            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-006", "Failed to fetch stats", e.getMessage());
        }
    }

    /**
     * Helper: Log an audit event
     */
    private void logAuditEvent(String action, UserEntity admin, String targetType, Long targetId, String details) {
        AuditLogEntity log = new AuditLogEntity();
        log.setAction(action);
        log.setPerformedById(admin.getId());
        log.setPerformedByEmail(admin.getEmail());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
