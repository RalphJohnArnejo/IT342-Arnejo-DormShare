package edu.cit.arnejo.dormshare.notification;

import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.notification.entity.NotificationEntity;
import edu.cit.arnejo.dormshare.notification.NotificationRepository;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;

    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              EmailNotificationService emailNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public void create(Long userId, String type, String title, String body) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setIsRead(false);
        notificationRepository.save(n);

        // Send email notification if enabled
        sendEmailNotificationForType(userId, type, title, body);
    }

    @Transactional
    public void createForUsers(List<Long> userIds, Long excludeUserId, String type, String title, String body) {
        for (Long userId : userIds) {
            if (userId == null) continue;
            if (excludeUserId != null && excludeUserId.equals(userId)) continue;
            create(userId, type, title, body);
        }
    }

    public ApiResponse list(Long userId, boolean unreadOnly, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        PageRequest page = PageRequest.of(0, safeLimit);

        List<NotificationEntity> results = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, page)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page);

        return ApiResponse.ok(results);
    }

    public ApiResponse unreadCount(Long userId) {
        return ApiResponse.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public ApiResponse markRead(Long userId, Long notificationId) {
        Optional<NotificationEntity> nOpt = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (nOpt.isEmpty()) {
            return ApiResponse.error("DB-001", "Resource not found", "Notification not found");
        }
        NotificationEntity n = nOpt.get();
        n.setIsRead(true);
        notificationRepository.save(n);
        return ApiResponse.ok(n);
    }

    @Transactional
    public ApiResponse markAllRead(Long userId) {
        List<NotificationEntity> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, PageRequest.of(0, 500));
        for (NotificationEntity n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
        return ApiResponse.ok(null);
    }

    /**
     * Send email notification based on notification type
     */
    private void sendEmailNotificationForType(Long userId, String type, String title, String body) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return;
        }

        UserEntity user = userOpt.get();

        // Route to appropriate email notification method based on type
        switch (type) {
            case "EXPENSE_CREATED" -> emailNotificationService.sendNotification(user, type, title, body);
            case "GROUP_INVITATION" -> emailNotificationService.sendNotification(user, type, title, body);
            case "SETTLEMENT_REMINDER" -> emailNotificationService.sendNotification(user, type, title, body);
            case "GROUP_MEMBER_ADDED" -> emailNotificationService.sendNotification(user, type, title, body);
            case "PAYMENT_RECEIVED" -> emailNotificationService.sendNotification(user, type, title, body);
            default -> {} // Silently ignore unknown types
        }
    }
}
