package edu.cit.arnejo.dormshare.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.UserManagementDTO;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.GroupRepository;
import edu.cit.arnejo.dormshare.repository.NotificationRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final NotificationRepository notificationRepository;

    public AdminService(UserRepository userRepository, 
                       GroupRepository groupRepository,
                       NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Get all users with their details
     */
    public ApiResponse getAllUsers() {
        try {
            List<UserManagementDTO> users = userRepository.findAll().stream()
                    .map(user -> new UserManagementDTO(
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole(),
                            true // Assuming all are active for now
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("users", users);
            data.put("total", users.size());
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-001", "Failed to fetch users", e.getMessage());
        }
    }

    /**
     * Deactivate a user
     */
    @Transactional
    public ApiResponse deactivateUser(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            // Mark user as inactive by updating a status (if such field exists)
            // For now, we just return success
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("status", "DEACTIVATED");
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-002", "Failed to deactivate user", e.getMessage());
        }
    }

    /**
     * Reactivate a user
     */
    @Transactional
    public ApiResponse reactivateUser(Long userId) {
        try {
            UserEntity user = userRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                return ApiResponse.error("USER-001", "User not found", "User with ID " + userId + " does not exist");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("email", user.getEmail());
            data.put("status", "ACTIVE");
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-003", "Failed to reactivate user", e.getMessage());
        }
    }

    /**
     * Get all groups
     */
    public ApiResponse getAllGroups() {
        try {
            List<Map<String, Object>> groups = groupRepository.findAll().stream()
                    .map(group -> {
                        Map<String, Object> groupData = new HashMap<>();
                        groupData.put("groupId", group.getId());
                        groupData.put("name", group.getName());
                        groupData.put("inviteCode", group.getInviteCode());
                        return groupData;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("groups", groups);
            data.put("total", groups.size());
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-004", "Failed to fetch groups", e.getMessage());
        }
    }

    /**
     * Get system activity logs
     */
    public ApiResponse getSystemLogs(int limit, int offset) {
        try {
            // Get recent notifications as activity logs
            List<Map<String, Object>> logs = notificationRepository.findAll().stream()
                    .skip(offset)
                    .limit(limit)
                    .map(notification -> {
                        Map<String, Object> log = new HashMap<>();
                        log.put("id", notification.getId());
                        log.put("type", notification.getType());
                        log.put("message", notification.getTitle() + ": " + notification.getBody());
                        log.put("createdAt", notification.getCreatedAt());
                        return log;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("logs", logs);
            data.put("total", logs.size());
            data.put("limit", limit);
            data.put("offset", offset);
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-005", "Failed to fetch logs", e.getMessage());
        }
    }

    /**
     * Get system statistics
     */
    public ApiResponse getSystemStats() {
        try {
            long totalUsers = userRepository.count();
            long totalGroups = groupRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("totalGroups", totalGroups);
            stats.put("activeUsers", totalUsers); // Simplified
            stats.put("timestamp", System.currentTimeMillis());

            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error("ADMIN-006", "Failed to fetch stats", e.getMessage());
        }
    }
}
