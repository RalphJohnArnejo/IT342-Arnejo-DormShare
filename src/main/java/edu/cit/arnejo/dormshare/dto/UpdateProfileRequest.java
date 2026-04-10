package edu.cit.arnejo.dormshare.dto;

public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;

    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
}
