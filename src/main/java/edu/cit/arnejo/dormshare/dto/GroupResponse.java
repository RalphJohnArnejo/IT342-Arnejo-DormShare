package edu.cit.arnejo.dormshare.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupResponse {
    private Long id;
    private String name;
    private String inviteCode;
    private List<MemberInfo> members;
    private LocalDateTime createdAt;

    public static class MemberInfo {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private LocalDateTime joinedAt;

        // Getters
        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public LocalDateTime getJoinedAt() { return joinedAt; }

        // Setters
        public void setUserId(Long userId) { this.userId = userId; }
        public void setName(String name) { this.name = name; }
        public void setEmail(String email) { this.email = email; }
        public void setRole(String role) { this.role = role; }
        public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public List<MemberInfo> getMembers() { return members; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public void setMembers(List<MemberInfo> members) { this.members = members; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
