package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     * - Validates required fields
     * - Checks for duplicate email
     * - Hashes password with BCrypt
     * - Saves to database
     */
    public Map<String, Object> registerUser(String name, String email, String password) {
        Map<String, Object> response = new HashMap<>();

        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Name is required.");
            return response;
        }
        if (email == null || email.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required.");
            return response;
        }
        if (password == null || password.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Password is required.");
            return response;
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email is already registered.");
            return response;
        }

        // Create and save user with hashed password
        UserEntity user = new UserEntity();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully.");
        return response;
    }

    /**
     * Login an existing user.
     * - Finds user by email
     * - Verifies password with BCrypt
     * - Returns user info on success
     */
    public Map<String, Object> loginUser(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email and password are required.");
            return response;
        }

        Optional<UserEntity> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());

        if (optionalUser.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return response;
        }

        UserEntity user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return response;
        }

        response.put("success", true);
        response.put("message", "Login successful.");
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        return response;
    }
}
