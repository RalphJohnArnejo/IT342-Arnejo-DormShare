package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.entity.NotificationEntity;
import edu.cit.arnejo.dormshare.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
}
