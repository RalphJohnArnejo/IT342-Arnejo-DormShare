package edu.cit.arnejo.dormshare.dto;

public class PaymentInitiateRequest {
    private Long payeeId;
    private Double amount;
    private Long groupId;
    private String description;

    public PaymentInitiateRequest() {}

    public PaymentInitiateRequest(Long payeeId, Double amount, Long groupId, String description) {
        this.payeeId = payeeId;
        this.amount = amount;
        this.groupId = groupId;
        this.description = description;
    }

    // Getters/Setters
    public Long getPayeeId() { return payeeId; }
    public void setPayeeId(Long payeeId) { this.payeeId = payeeId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
