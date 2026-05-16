package edu.cit.arnejo.dormshare.notification;

import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing user email notification preferences
 */
@RestController
@RequestMapping("/api/email-notification-preferences")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class EmailNotificationPreferenceController {

    private final EmailNotificationPreferenceService preferenceService;

    public EmailNotificationPreferenceController(EmailNotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
     * Get all email notification preferences for the authenticated user
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getPreferences(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        return ResponseEntity.ok(preferenceService.getUserPreferences(user.getId()));
    }

    /**
     * Update a specific email notification preference
     */
    @PatchMapping("/{notificationType}")
    public ResponseEntity<ApiResponse> updatePreference(
            @PathVariable String notificationType,
            @RequestBody Map<String, Boolean> payload,
            @AuthenticationPrincipal UserEntity user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        Boolean enabled = payload.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALID-001", "Validation failed", "enabled field is required"));
        }

        ApiResponse result = preferenceService.updatePreference(user.getId(), notificationType, enabled);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Update multiple email notification preferences at once
     */
    @PatchMapping("/batch")
    public ResponseEntity<ApiResponse> updatePreferencesBatch(
            @RequestBody Map<String, Boolean> preferencesMap,
            @AuthenticationPrincipal UserEntity user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (preferencesMap == null || preferencesMap.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALID-002", "Validation failed", "Preferences map is required"));
        }

        ApiResponse result = preferenceService.updatePreferencesBatch(user.getId(), preferencesMap);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Enable all email notifications for the user
     */
    @PostMapping("/enable-all")
    public ResponseEntity<ApiResponse> enableAllNotifications(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        ApiResponse result = preferenceService.enableAllNotifications(user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Disable all email notifications for the user
     */
    @PostMapping("/disable-all")
    public ResponseEntity<ApiResponse> disableAllNotifications(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        ApiResponse result = preferenceService.disableAllNotifications(user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }
}
