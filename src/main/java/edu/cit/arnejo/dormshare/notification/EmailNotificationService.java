package edu.cit.arnejo.dormshare.notification;

import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.notification.entity.EmailNotificationPreferenceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Enhanced Email Notification Service
 * Handles sending templated email notifications with preference checking
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailNotificationPreferenceRepository preferenceRepository;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailNotificationService(JavaMailSender mailSender,
                                    EmailNotificationPreferenceRepository preferenceRepository) {
        this.mailSender = mailSender;
        this.preferenceRepository = preferenceRepository;
        log.info("EmailNotificationService initialized");
    }

    /**
     * Send a templated email notification if user has enabled this notification type
     */
    public void sendNotification(UserEntity recipient, String notificationType, String subject, String body) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("Cannot send notification: recipient email is invalid");
            return;
        }

        log.debug("Checking email notification preference for user {} ({}), type: {}", 
            recipient.getId(), recipient.getEmail(), notificationType);

        if (!isEmailNotificationEnabled(recipient.getId(), notificationType)) {
            log.debug("Email notification type {} is disabled for user {}", notificationType, recipient.getId());
            return;
        }

        log.debug("Sending email notification to {}", recipient.getEmail());
        sendEmail(recipient.getEmail(), subject, body);
    }

    /**
     * Send email notification for expense creation
     */
    public void sendExpenseCreatedNotification(UserEntity recipient, String expenseTitle, String creatorName, String groupName) {
        String subject = "DormShare: New Expense in " + groupName;
        String body = buildEmailTemplate(
                "Hi " + recipient.getFirstName() + ",\n\n",
                "A new expense has been created in your group.\n\n",
                "Expense: " + expenseTitle + "\n" +
                        "Created by: " + creatorName + "\n" +
                        "Group: " + groupName + "\n\n",
                "Log in to view the details and settle your share."
        );

        sendNotification(recipient, "EXPENSE_CREATED", subject, body);
    }

    /**
     * Send email notification for group invitation
     */
    public void sendGroupInvitationNotification(UserEntity recipient, String inviterName, String groupName) {
        String subject = "DormShare: You're invited to " + groupName;
        String body = buildEmailTemplate(
                "Hi " + recipient.getFirstName() + ",\n\n",
                inviterName + " invited you to join a group on DormShare!\n\n",
                "Group: " + groupName + "\n\n",
                "Log in to view the invitation and join the group."
        );

        sendNotification(recipient, "GROUP_INVITATION", subject, body);
    }

    /**
     * Send email notification for settlement reminder
     */
    public void sendSettlementReminderNotification(UserEntity recipient, String creditorName, String amount, String expenseTitle) {
        String subject = "DormShare: Settlement Reminder";
        String body = buildEmailTemplate(
                "Hi " + recipient.getFirstName() + ",\n\n",
                "You have an outstanding payment to settle.\n\n",
                "Amount: ₱" + amount + "\n" +
                        "Creditor: " + creditorName + "\n" +
                        "Expense: " + expenseTitle + "\n\n",
                "Log in to mark the payment as settled."
        );

        sendNotification(recipient, "SETTLEMENT_REMINDER", subject, body);
    }

    /**
     * Send email notification for user registration
     */
    public void sendWelcomeNotification(UserEntity user) {
        String subject = "Welcome to DormShare!";
        String body = buildEmailTemplate(
                "Hi " + user.getFirstName() + ",\n\n",
                "Welcome to DormShare! We're excited to have you on board.\n\n",
                "DormShare helps you manage shared expenses with your roommates.\n\n",
                "Get started by creating or joining a group. Happy tracking!"
        );

        sendEmail(user.getEmail(), subject, body);
    }

    /**
     * Send email notification for account changes
     */
    public void sendAccountChangeNotification(UserEntity user, String changeType) {
        String subject = "DormShare: Account Update";
        String message = switch (changeType) {
            case "PASSWORD_CHANGED" -> "Your password has been successfully changed.";
            case "EMAIL_UPDATED" -> "Your email address has been updated.";
            case "PROFILE_UPDATED" -> "Your profile information has been updated.";
            default -> "Your account has been updated.";
        };

        String body = buildEmailTemplate(
                "Hi " + user.getFirstName() + ",\n\n",
                message + "\n\n",
                "",
                "If you didn't make this change, please contact us immediately."
        );

        sendEmail(user.getEmail(), subject, body);
    }

    /**
     * Send email notification for group member added
     */
    public void sendGroupMemberAddedNotification(UserEntity newMember, String groupName, String addedByName) {
        String subject = "DormShare: Added to " + groupName;
        String body = buildEmailTemplate(
                "Hi " + newMember.getFirstName() + ",\n\n",
                addedByName + " added you to the group '" + groupName + "' on DormShare.\n\n",
                "",
                "Log in to view the group and start tracking expenses."
        );

        sendNotification(newMember, "GROUP_MEMBER_ADDED", subject, body);
    }

    /**
     * Send email notification for group member removal
     */
    public void sendGroupMemberRemovedNotification(UserEntity removedMember, String groupName) {
        String subject = "DormShare: Removed from " + groupName;
        String body = buildEmailTemplate(
                "Hi " + removedMember.getFirstName() + ",\n\n",
                "You have been removed from the group '" + groupName + "'.\n\n",
                "",
                "Log in to view your remaining groups."
        );

        sendEmail(removedMember.getEmail(), subject, body);
    }

    /**
     * Send email notification for payment received
     */
    public void sendPaymentReceivedNotification(UserEntity recipient, String senderName, String amount, String expenseTitle) {
        String subject = "DormShare: Payment Received";
        String body = buildEmailTemplate(
                "Hi " + recipient.getFirstName() + ",\n\n",
                senderName + " has sent you a payment.\n\n",
                "Amount: ₱" + amount + "\n" +
                        "For: " + expenseTitle + "\n\n",
                "Log in to confirm the payment."
        );

        sendNotification(recipient, "PAYMENT_RECEIVED", subject, body);
    }

    /**
     * Check if email notification is enabled for user
     */
    private boolean isEmailNotificationEnabled(Long userId, String notificationType) {
        var pref = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);
        boolean enabled = pref.map(EmailNotificationPreferenceEntity::getEnabled).orElse(true); // Default to enabled
        log.debug("Email preference for user {} type {}: {} (found: {})", userId, notificationType, enabled, pref.isPresent());
        return enabled;
    }

    /**
     * Build formatted email body template
     */
    private String buildEmailTemplate(String greeting, String mainContent, String details, String closing) {
        return greeting +
                mainContent +
                details +
                closing + "\n\n" +
                "— DormShare Team\n\n" +
                "Visit us: " + frontendUrl;
    }

    /**
     * Check if SMTP is configured
     */
    private boolean isSmtpConfigured() {
        boolean configured = smtpHost != null && !smtpHost.isBlank() &&
                fromAddress != null && !fromAddress.isBlank();
        if (!configured) {
            log.debug("SMTP not configured: smtpHost='{}', fromAddress='{}'", smtpHost, fromAddress);
        }
        return configured;
    }

    /**
     * Send raw email
     */
    private void sendEmail(String to, String subject, String body) {
        log.debug("sendEmail called: to={}, smtpHost={}, fromAddress={}", to, smtpHost, fromAddress);
        
        if (!isSmtpConfigured()) {
            log.info("SMTP not configured; skipping email to {}", to);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
