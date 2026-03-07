package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.config.JwtUtil;
import edu.cit.arnejo.dormshare.dto.AuthResponse;
import edu.cit.arnejo.dormshare.dto.LoginRequest;
import edu.cit.arnejo.dormshare.dto.RegisterRequest;
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
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * Validates fields, checks duplicate email, hashes password with BCrypt, saves to DB.
     */
    public AuthResponse registerUser(RegisterRequest request) {
        // Validate required fields
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            return AuthResponse.error("VALID-001", "Validation failed", "First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            return AuthResponse.error("VALID-001", "Validation failed", "Last name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return AuthResponse.error("VALID-001", "Validation failed", "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            return AuthResponse.error("VALID-001", "Validation failed", "Password must be at least 8 characters");
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            return AuthResponse.error("DB-002", "Duplicate entry", "Email is already registered");
        }

        // Create and save user
        UserEntity user = new UserEntity();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("role", user.getRole());

        return AuthResponse.ok(data);
    }

    /**
     * Login an existing user.
     * Finds by email, verifies password with BCrypt, returns JWT token.
     */
    public AuthResponse loginUser(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return AuthResponse.error("VALID-001", "Validation failed", "Email and password are required");
        }

        Optional<UserEntity> optionalUser = userRepository.findByEmail(request.getEmail().trim().toLowerCase());

        if (optionalUser.isEmpty()) {
            return AuthResponse.error("AUTH-001", "Invalid credentials", "Email or password is incorrect");
        }

        UserEntity user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return AuthResponse.error("AUTH-001", "Invalid credentials", "Email or password is incorrect");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("role", user.getRole());

        return AuthResponse.ok(data);
    }
}
