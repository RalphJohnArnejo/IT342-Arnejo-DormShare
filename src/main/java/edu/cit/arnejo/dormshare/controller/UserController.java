package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.ChangePasswordRequest;
import edu.cit.arnejo.dormshare.dto.UpdateProfileRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * GET /api/users/me
     * Get current user's profile information.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getProfile(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * PATCH /api/users/me
     * Update current user's profile (first name, last name, email).
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        // Update fields if provided
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            // Check if email is already taken by someone else
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("DB-002", "Duplicate entry", "Email is already in use"));
            }
            user.setEmail(newEmail);
        }

        userRepository.save(user);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * POST /api/users/change-password
     * Change the current user's password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALID-001", "Validation failed", "Current password is required"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALID-001", "Validation failed", "New password must be at least 8 characters"));
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("AUTH-002", "Wrong password", "Current password is incorrect"));
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }
}
