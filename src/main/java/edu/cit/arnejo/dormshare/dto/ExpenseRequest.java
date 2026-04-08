package edu.cit.arnejo.dormshare.dto;

import java.math.BigDecimal;
import java.util.List;

public class ExpenseRequest {
    private BigDecimal amount;
    private String description;
    private Long paidById;
    private String category;
    private List<SplitRequest> splits;

    public static class SplitRequest {
        private Long userId;
        private BigDecimal amount;

        // Getters/Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    // Getters/Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPaidById() { return paidById; }
    public void setPaidById(Long paidById) { this.paidById = paidById; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<SplitRequest> getSplits() { return splits; }
    public void setSplits(List<SplitRequest> splits) { this.splits = splits; }
}
