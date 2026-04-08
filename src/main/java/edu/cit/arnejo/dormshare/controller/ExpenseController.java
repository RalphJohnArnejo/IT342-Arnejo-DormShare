package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.ExpenseRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.ExpenseService;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    public ExpenseController(ExpenseService expenseService, UserRepository userRepository) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> logExpense(
            @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        // Force the paidById to be the current user for security
        request.setPaidById(user.getId());
        
        ApiResponse result = expenseService.createExpense(request);
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse> getLedger() {
        ApiResponse result = expenseService.getLedger();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getSummary(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = expenseService.getSummary(user.getId());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/settle/{splitId}")
    public ResponseEntity<ApiResponse> settleSplit(
            @PathVariable Long splitId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = expenseService.settleSplit(splitId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllRoommates() {
        // Return a list of all users as potential roommates for splitting
        return ResponseEntity.ok(ApiResponse.ok(
            userRepository.findAll().stream()
                .map(u -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getFirstName() + " " + u.getLastName());
                    map.put("email", u.getEmail());
                    return map;
                }).collect(Collectors.toList())
        ));
    }
}
