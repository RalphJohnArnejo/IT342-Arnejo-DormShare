package edu.cit.arnejo.dormshare.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_owed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountOwed;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;

    // Getters
    public Long getId() { return id; }
    public Long getExpenseId() { return expenseId; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmountOwed() { return amountOwed; }
    public Boolean getIsSettled() { return isSettled; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAmountOwed(BigDecimal amountOwed) { this.amountOwed = amountOwed; }
    public void setIsSettled(Boolean isSettled) { this.isSettled = isSettled; }
}
