package edu.cit.arnejo.dormshare.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.arnejo.dormshare.notification.entity.EmailNotificationPreferenceEntity;

@Repository
public interface EmailNotificationPreferenceRepository extends JpaRepository<EmailNotificationPreferenceEntity, Long> {
    Optional<EmailNotificationPreferenceEntity> findByUserIdAndNotificationType(Long userId, String notificationType);

    List<EmailNotificationPreferenceEntity> findByUserId(Long userId);

    List<EmailNotificationPreferenceEntity> findByUserIdAndEnabled(Long userId, Boolean enabled);
}
