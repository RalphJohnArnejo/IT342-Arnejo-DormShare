package edu.cit.arnejo.dormshare.dto;

public class PantryItemRequest {
    private String itemName;
    private String category;
    private String status;
    private Double quantity;

    // Getters
    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public Double getQuantity() { return quantity; }

    // Setters
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(String status) { this.status = status; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
