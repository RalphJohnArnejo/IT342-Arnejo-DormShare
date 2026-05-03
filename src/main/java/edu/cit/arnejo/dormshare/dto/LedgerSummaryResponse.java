package edu.cit.arnejo.dormshare.dto;

import java.util.List;
import java.util.Map;

public class LedgerSummaryResponse {
    private Long groupId;
    private String groupName;
    private Map<Long, String> userNames;
    private List<DebtSummary> debts; // Who owes whom
    private Map<String, Double> userBalances; // Positive = they owe you, Negative = you owe them

    public static class DebtSummary {
        private Long fromUserId;
        private String fromUserName;
        private Long toUserId;
        private String toUserName;
        private Double amount;
        private String status; // PENDING, SETTLED

        public DebtSummary() {}

        public DebtSummary(Long fromUserId, String fromUserName, Long toUserId, String toUserName, Double amount, String status) {
            this.fromUserId = fromUserId;
            this.fromUserName = fromUserName;
            this.toUserId = toUserId;
            this.toUserName = toUserName;
            this.amount = amount;
            this.status = status;
        }

        // Getters/Setters
        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

        public String getFromUserName() { return fromUserName; }
        public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

        public Long getToUserId() { return toUserId; }
        public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

        public String getToUserName() { return toUserName; }
        public void setToUserName(String toUserName) { this.toUserName = toUserName; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Constructors
    public LedgerSummaryResponse() {}

    // Getters/Setters
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Map<Long, String> getUserNames() { return userNames; }
    public void setUserNames(Map<Long, String> userNames) { this.userNames = userNames; }

    public List<DebtSummary> getDebts() { return debts; }
    public void setDebts(List<DebtSummary> debts) { this.debts = debts; }

    public Map<String, Double> getUserBalances() { return userBalances; }
    public void setUserBalances(Map<String, Double> userBalances) { this.userBalances = userBalances; }
}
