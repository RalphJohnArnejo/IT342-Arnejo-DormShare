package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.AuthResponse;
import edu.cit.arnejo.dormshare.dto.LoginRequest;
import edu.cit.arnejo.dormshare.dto.RegisterRequest;
import edu.cit.arnejo.dormshare.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /auth/register
     * Registers a new user with email, password, first name, and last name.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse result = userService.registerUser(request);

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            String errorCode = result.getError().getCode();
            if ("DB-002".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    /**
     * POST /auth/login
     * Authenticates a user and returns an access token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse result = userService.loginUser(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }
}
