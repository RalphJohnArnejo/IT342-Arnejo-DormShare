package edu.cit.arnejo.dormshare.dto;

public class StripePaymentConfirmRequest {
    private String paymentIntentId;
    private String paymentMethodId;
    private Long settlementId;
    private String description;

    public StripePaymentConfirmRequest() {}

    public StripePaymentConfirmRequest(String paymentIntentId, String paymentMethodId, 
                                     Long settlementId, String description) {
        this.paymentIntentId = paymentIntentId;
        this.paymentMethodId = paymentMethodId;
        this.settlementId = settlementId;
        this.description = description;
    }

    // Getters/Setters
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
