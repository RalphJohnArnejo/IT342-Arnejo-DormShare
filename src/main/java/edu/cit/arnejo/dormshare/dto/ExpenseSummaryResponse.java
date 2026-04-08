package edu.cit.arnejo.dormshare.dto;

import java.math.BigDecimal;

public class ExpenseSummaryResponse {
    private BigDecimal owedToYou = BigDecimal.ZERO;
    private BigDecimal youOwe = BigDecimal.ZERO;
    private BigDecimal netBalance = BigDecimal.ZERO;

    public ExpenseSummaryResponse(BigDecimal owedToYou, BigDecimal youOwe) {
        this.owedToYou = owedToYou != null ? owedToYou : BigDecimal.ZERO;
        this.youOwe = youOwe != null ? youOwe : BigDecimal.ZERO;
        this.netBalance = this.owedToYou.subtract(this.youOwe);
    }

    public BigDecimal getOwedToYou() { return owedToYou; }
    public BigDecimal getYouOwe() { return youOwe; }
    public BigDecimal getNetBalance() { return netBalance; }
}
