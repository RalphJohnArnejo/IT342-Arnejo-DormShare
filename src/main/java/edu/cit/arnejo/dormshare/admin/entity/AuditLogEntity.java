package edu.cit.arnejo.dormshare.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(name = "performed_by_id")
    private Long performedById;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getAction() { return action; }
    public Long getPerformedById() { return performedById; }
    public String getPerformedByEmail() { return performedByEmail; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setAction(String action) { this.action = action; }
    public void setPerformedById(Long performedById) { this.performedById = performedById; }
    public void setPerformedByEmail(String performedByEmail) { this.performedByEmail = performedByEmail; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setDetails(String details) { this.details = details; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
