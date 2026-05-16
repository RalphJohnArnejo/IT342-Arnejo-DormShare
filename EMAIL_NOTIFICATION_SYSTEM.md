# Email Notification System Documentation

## Overview

The Email Notification System enhances DormShare with automated email notifications for important events. Users can customize which notifications they want to receive via email, and the system ensures timely communication about expense-related activities, group invitations, and settlements.

## Features

### 1. **Automated Email Notifications**
- Sends emails for key events in the application
- Respects user preferences
- Gracefully handles SMTP configuration

### 2. **User-Configurable Preferences**
- Users can enable/disable individual notification types
- Batch update capabilities
- Enable/disable all notifications at once

### 3. **Supported Notification Types**

| Type | Trigger | Recipients |
|------|---------|-----------|
| `EXPENSE_CREATED` | When an expense is created | All group members (excluding creator) |
| `GROUP_INVITATION` | When invited to a group | Invited user |
| `SETTLEMENT_REMINDER` | Settlement request | Debtor |
| `GROUP_MEMBER_ADDED` | When added to a group | New member |
| `PAYMENT_RECEIVED` | Payment confirmation | Creditor |

### 4. **Welcome Email**
- Automatically sent when user registers
- Contains getting started information

## Architecture

### Components

#### 1. **EmailNotificationService** (`notification/EmailNotificationService.java`)
- Manages all email sending operations
- Template generation for different notification types
- Preference checking before sending
- SMTP configuration handling

#### 2. **EmailNotificationPreferenceService** (`notification/EmailNotificationPreferenceService.java`)
- Manages user email notification preferences
- Initialize default preferences for new users
- Enable/disable individual or all notifications

#### 3. **EmailNotificationPreferenceController** (`notification/EmailNotificationPreferenceController.java`)
- REST API endpoints for preference management
- User authentication and authorization

#### 4. **EmailNotificationPreferenceEntity** (`notification/entity/EmailNotificationPreferenceEntity.java`)
- JPA entity for storing user preferences
- Database table: `email_notification_preferences`

#### 5. **NotificationService** (Enhanced)
- Updated to send emails alongside in-app notifications
- Routes notifications to appropriate email senders

## Database Schema

### email_notification_preferences Table

```sql
CREATE TABLE email_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, notification_type)
);
```

## API Endpoints

### Get User Preferences

```http
GET /api/email-notification-preferences
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "EXPENSE_CREATED": {
      "id": 1,
      "enabled": true,
      "updatedAt": "2026-05-16T10:30:00"
    },
    "GROUP_INVITATION": {
      "id": 2,
      "enabled": true,
      "updatedAt": "2026-05-16T10:30:00"
    }
  }
}
```

### Update Specific Preference

```http
PATCH /api/email-notification-preferences/{notificationType}
Authorization: Bearer {token}
Content-Type: application/json

{
  "enabled": false
}
```

### Update Multiple Preferences

```http
PATCH /api/email-notification-preferences/batch
Authorization: Bearer {token}
Content-Type: application/json

{
  "EXPENSE_CREATED": false,
  "GROUP_INVITATION": true,
  "SETTLEMENT_REMINDER": true
}
```

### Enable All Notifications

```http
POST /api/email-notification-preferences/enable-all
Authorization: Bearer {token}
```

### Disable All Notifications

```http
POST /api/email-notification-preferences/disable-all
Authorization: Bearer {token}
```

## Configuration

### Environment Variables

Required for email functionality:

```bash
# SMTP Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=noreply@dormshare.app

# Frontend URL (for email links)
FRONTEND_URL=https://dormshare.app
```

### Application Properties

```properties
# Global email notification settings
app.email-notifications.enabled=true

# Individual notification type settings
app.email-notifications.expense-created=true
app.email-notifications.group-invitation=true
app.email-notifications.settlement-reminder=true
app.email-notifications.group-member-added=true
app.email-notifications.payment-received=true
```

## Usage Examples

### Sending Expense Notification

```java
@Autowired
private EmailNotificationService emailNotificationService;

// Send to all group members
emailNotificationService.sendExpenseCreatedNotification(
    user,
    "Dinner at Restaurant X",
    "John Doe",
    "Apartment 101 Roommates"
);
```

### Sending Group Invitation

```java
emailNotificationService.sendGroupInvitationNotification(
    invitedUser,
    "Jane Smith",
    "Summer Internship Group"
);
```

### Initializing Preferences for New User

```java
@Autowired
private EmailNotificationPreferenceService preferenceService;

preferenceService.initializeDefaultPreferences(userId);
```

## Error Handling

### Graceful Degradation

- If SMTP is not configured, email sending is logged and skipped
- Invalid user email addresses are logged but don't crash the system
- Notification type validation prevents invalid entries

### Logging

The system logs:
- Email sent successfully: `INFO` level
- SMTP not configured: `INFO` level
- Failed email send attempts: `WARN` level with user email and error

## Security Considerations

1. **Authentication Required**: All preference endpoints require user authentication
2. **User Isolation**: Users can only modify their own preferences
3. **Input Validation**: Notification types are validated against a whitelist
4. **Email Privacy**: Only registered user emails are used

## Testing

### Unit Tests

```java
@Test
void testEmailNotificationPreferenceCreation() {
    // Verify preferences are initialized for new users
}

@Test
void testEmailNotificationDisabled() {
    // Verify emails not sent when disabled
}

@Test
void testPreferenceBatchUpdate() {
    // Verify batch updates work correctly
}
```

### Integration Tests

```java
@Test
void testExpenseNotificationEmailSent() {
    // Create expense and verify email sent
}

@Test
void testGroupInvitationEmail() {
    // Send invitation and verify email sent
}
```

## Troubleshooting

### Emails Not Sending

1. **Check SMTP Configuration**
   ```bash
   # Verify environment variables are set
   echo $SMTP_HOST
   echo $SMTP_FROM
   ```

2. **Check Logs**
   ```
   grep "SMTP" application.log
   grep "Failed to send email" application.log
   ```

3. **Verify User Preferences**
   - Call `GET /api/email-notification-preferences` to check if notification type is enabled

### Database Issues

```sql
-- Check if preferences table exists
SELECT * FROM email_notification_preferences;

-- Check preferences for a user
SELECT * FROM email_notification_preferences WHERE user_id = 123;
```

## Future Enhancements

1. **Email Templates**: HTML email templates with branding
2. **Scheduled Digest**: Weekly/daily digest emails
3. **SMS Notifications**: SMS as alternative channel
4. **Webhook Notifications**: Custom webhook integration
5. **Notification History**: Audit trail of sent emails
6. **Rate Limiting**: Prevent email spam
7. **Template Customization**: Allow users to customize email content

## Maintenance

### Database Cleanup

```sql
-- Archive old preferences (older than 1 year)
DELETE FROM email_notification_preferences 
WHERE updated_at < NOW() - INTERVAL '1 year';
```

### Monitoring

Monitor these metrics:
- Email send success rate
- Failed email attempts
- SMTP connection errors
- User preference changes

## Support

For issues or feature requests related to email notifications, please create an issue on GitHub with:
- Error messages from logs
- Steps to reproduce
- Expected vs actual behavior
