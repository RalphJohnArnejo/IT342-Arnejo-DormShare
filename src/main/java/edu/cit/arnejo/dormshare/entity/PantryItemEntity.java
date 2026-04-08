package edu.cit.arnejo.dormshare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pantry_items")
public class PantryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String category = "Other";

    @Column(nullable = false)
    private String status = "IN";

    @Column(nullable = false)
    private Double quantity = 1.0;

    @Column(name = "added_by_id")
    private Long addedById;

    @Column(name = "added_by_name")
    private String addedByName;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(name = "updated_by_name")
    private String updatedByName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "IN";
        if (this.category == null) this.category = "Other";
        if (this.quantity == null) this.quantity = 1.0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public Double getQuantity() { return quantity; }
    public Long getAddedById() { return addedById; }
    public String getAddedByName() { return addedByName; }
    public Long getUpdatedById() { return updatedById; }
    public String getUpdatedByName() { return updatedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(String status) { this.status = status; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setAddedById(Long addedById) { this.addedById = addedById; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }
    public void setUpdatedById(Long updatedById) { this.updatedById = updatedById; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
