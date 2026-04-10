package edu.cit.arnejo.dormshare.dto;

public class GroupRequest {
    private String name;
    private String inviteCode;

    // Getters
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
