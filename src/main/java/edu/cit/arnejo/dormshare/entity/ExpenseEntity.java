package edu.cit.arnejo.dormshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Column(name = "paid_by_id", nullable = false)
    private Long paidById;

    @Column(name = "group_id")
    private Long groupId;

    @Column
    private String category;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.date == null) {
            this.date = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Long getPaidById() { return paidById; }
    public Long getGroupId() { return groupId; }
    public String getCategory() { return category; }
    public LocalDateTime getDate() { return date; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setPaidById(Long paidById) { this.paidById = paidById; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setCategory(String category) { this.category = category; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
