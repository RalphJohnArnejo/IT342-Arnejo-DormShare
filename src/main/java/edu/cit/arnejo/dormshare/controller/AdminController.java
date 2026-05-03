package edu.cit.arnejo.dormshare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.AdminService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Verify admin role
     */
    private ResponseEntity<ApiResponse> requireAdmin(UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        if (!user.getRole().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("PERM-001", "Permission denied", "Only admins can access this"));
        }
        return null;
    }

    /**
     * GET /api/admin/users
     * Retrieve all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllUsers(@AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.getAllUsers();
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/admin/users/{userId}/deactivate
     * Deactivate a user
     */
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse> deactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.deactivateUser(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/admin/users/{userId}/reactivate
     * Reactivate a user
     */
    @PatchMapping("/users/{userId}/reactivate")
    public ResponseEntity<ApiResponse> reactivateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.reactivateUser(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/groups
     * Retrieve all groups
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse> getAllGroups(@AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.getAllGroups();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/system/logs
     * Retrieve system activity logs
     */
    @GetMapping("/system/logs")
    public ResponseEntity<ApiResponse> getSystemLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.getSystemLogs(limit, offset);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/system/stats
     * Retrieve system statistics
     */
    @GetMapping("/system/stats")
    public ResponseEntity<ApiResponse> getSystemStats(@AuthenticationPrincipal UserEntity user) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(user);
        if (adminCheck != null) return adminCheck;

        ApiResponse result = adminService.getSystemStats();
        return ResponseEntity.ok(result);
    }
}
