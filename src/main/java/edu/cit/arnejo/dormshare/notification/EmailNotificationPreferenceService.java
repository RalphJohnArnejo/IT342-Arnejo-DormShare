package edu.cit.arnejo.dormshare.notification;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.arnejo.dormshare.notification.entity.EmailNotificationPreferenceEntity;
import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;

/**
 * Service to manage user email notification preferences
 */
@Service
public class EmailNotificationPreferenceService {

    private final EmailNotificationPreferenceRepository preferenceRepository;

    // Default notification types
    private static final List<String> DEFAULT_NOTIFICATION_TYPES = Arrays.asList(
            "EXPENSE_CREATED",
            "GROUP_INVITATION",
            "SETTLEMENT_REMINDER",
            "GROUP_MEMBER_ADDED",
            "PAYMENT_RECEIVED"
    );

    public EmailNotificationPreferenceService(EmailNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Initialize default email notification preferences for a new user
     */
    @Transactional
    public void initializeDefaultPreferences(Long userId) {
        for (String notificationType : DEFAULT_NOTIFICATION_TYPES) {
            var existingPref = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);
            if (existingPref.isEmpty()) {
                EmailNotificationPreferenceEntity pref = new EmailNotificationPreferenceEntity(
                        userId,
                        notificationType,
                        true // All notifications enabled by default
                );
                preferenceRepository.save(pref);
            }
        }
    }

    /**
     * Get all notification preferences for a user
     */
    public ApiResponse getUserPreferences(Long userId) {
        try {
            List<EmailNotificationPreferenceEntity> preferences = preferenceRepository.findByUserId(userId);

            // Ensure all default types are present
            ensureAllTypesPresent(userId, preferences);

            Map<String, Object> response = preferences.stream()
                    .collect(Collectors.toMap(
                            EmailNotificationPreferenceEntity::getNotificationType,
                            pref -> Map.of(
                                    "id", pref.getId(),
                                    "enabled", pref.getEnabled(),
                                    "updatedAt", pref.getUpdatedAt()
                            )
                    ));

            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("PREF-001", "Failed to fetch preferences", e.getMessage());
        }
    }

    /**
     * Update a specific notification preference
     */
    @Transactional
    public ApiResponse updatePreference(Long userId, String notificationType, Boolean enabled) {
        try {
            if (!DEFAULT_NOTIFICATION_TYPES.contains(notificationType)) {
                return ApiResponse.error("PREF-002", "Invalid notification type", "Unknown notification type: " + notificationType);
            }

            var pref = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);

            EmailNotificationPreferenceEntity preference;
            if (pref.isPresent()) {
                preference = pref.get();
                preference.setEnabled(enabled);
            } else {
                preference = new EmailNotificationPreferenceEntity(userId, notificationType, enabled);
            }

            preferenceRepository.save(preference);

            Map<String, Object> response = Map.of(
                    "id", preference.getId(),
                    "notificationType", preference.getNotificationType(),
                    "enabled", preference.getEnabled(),
                    "updatedAt", preference.getUpdatedAt()
            );

            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error("PREF-003", "Failed to update preference", e.getMessage());
        }
    }

    /**
     * Update multiple preferences at once
     */
    @Transactional
    public ApiResponse updatePreferencesBatch(Long userId, Map<String, Boolean> preferencesMap) {
        try {
            for (Map.Entry<String, Boolean> entry : preferencesMap.entrySet()) {
                updatePreference(userId, entry.getKey(), entry.getValue());
            }

            return getUserPreferences(userId);
        } catch (Exception e) {
            return ApiResponse.error("PREF-004", "Failed to update preferences", e.getMessage());
        }
    }

    /**
     * Check if a specific notification type is enabled for user
     */
    public boolean isNotificationEnabled(Long userId, String notificationType) {
        var pref = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);
        return pref.map(EmailNotificationPreferenceEntity::getEnabled).orElse(true); // Default to enabled
    }

    /**
     * Enable all notifications for a user
     */
    @Transactional
    public ApiResponse enableAllNotifications(Long userId) {
        try {
            List<EmailNotificationPreferenceEntity> preferences = preferenceRepository.findByUserId(userId);
            for (EmailNotificationPreferenceEntity pref : preferences) {
                pref.setEnabled(true);
            }
            preferenceRepository.saveAll(preferences);
            return getUserPreferences(userId);
        } catch (Exception e) {
            return ApiResponse.error("PREF-005", "Failed to enable all notifications", e.getMessage());
        }
    }

    /**
     * Disable all notifications for a user
     */
    @Transactional
    public ApiResponse disableAllNotifications(Long userId) {
        try {
            List<EmailNotificationPreferenceEntity> preferences = preferenceRepository.findByUserId(userId);
            for (EmailNotificationPreferenceEntity pref : preferences) {
                pref.setEnabled(false);
            }
            preferenceRepository.saveAll(preferences);
            return getUserPreferences(userId);
        } catch (Exception e) {
            return ApiResponse.error("PREF-006", "Failed to disable all notifications", e.getMessage());
        }
    }

    /**
     * Ensure all default notification types exist for user
     */
    private void ensureAllTypesPresent(Long userId, List<EmailNotificationPreferenceEntity> existingPrefs) {
        var existingTypes = existingPrefs.stream()
                .map(EmailNotificationPreferenceEntity::getNotificationType)
                .collect(Collectors.toSet());

        for (String notificationType : DEFAULT_NOTIFICATION_TYPES) {
            if (!existingTypes.contains(notificationType)) {
                EmailNotificationPreferenceEntity pref = new EmailNotificationPreferenceEntity(
                        userId,
                        notificationType,
                        true
                );
                preferenceRepository.save(pref);
            }
        }
    }
}
