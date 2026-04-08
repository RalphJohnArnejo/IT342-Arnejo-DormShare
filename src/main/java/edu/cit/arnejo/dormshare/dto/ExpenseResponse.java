package edu.cit.arnejo.dormshare.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private Long paidById;
    private String payerName;
    private String category;
    private LocalDateTime date;
    private List<SplitResponse> splits;

    public static class SplitResponse {
        private Long id;
        private Long userId;
        private String userName;
        private BigDecimal amountOwed;
        private Boolean isSettled;

        // Getters/Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public BigDecimal getAmountOwed() { return amountOwed; }
        public void setAmountOwed(BigDecimal amountOwed) { this.amountOwed = amountOwed; }
        public Boolean getIsSettled() { return isSettled; }
        public void setIsSettled(Boolean isSettled) { this.isSettled = isSettled; }
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getPaidById() { return paidById; }
    public void setPaidById(Long paidById) { this.paidById = paidById; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public List<SplitResponse> getSplits() { return splits; }
    public void setSplits(List<SplitResponse> splits) { this.splits = splits; }
}
