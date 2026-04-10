package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.ExpenseRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.ExpenseService;
import edu.cit.arnejo.dormshare.service.GroupService;
import edu.cit.arnejo.dormshare.repository.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import edu.cit.arnejo.dormshare.entity.GroupMembershipEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final GroupMembershipRepository membershipRepository;

    public ExpenseController(ExpenseService expenseService,
                             UserRepository userRepository,
                             GroupService groupService,
                             GroupMembershipRepository membershipRepository) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.membershipRepository = membershipRepository;
    }

    /**
     * Helper: return an error response when user has no group.
     */
    private ResponseEntity<ApiResponse> noGroupError() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("GROUP-003", "Not in a group",
                        "You must join or create a group before using expenses"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> logExpense(
            @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long groupId = groupService.getUserGroupId(user.getId());
        if (groupId == null) return noGroupError();

        // Force the paidById to be the current user for security
        request.setPaidById(user.getId());
        
        ApiResponse result = expenseService.createExpense(request, groupId);
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse> getLedger(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long groupId = groupService.getUserGroupId(user.getId());
        if (groupId == null) return noGroupError();

        ApiResponse result = expenseService.getLedger(groupId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getSummary(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long groupId = groupService.getUserGroupId(user.getId());
        if (groupId == null) return noGroupError();

        ApiResponse result = expenseService.getSummary(user.getId(), groupId);
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
    public ResponseEntity<ApiResponse> getAllRoommates(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long groupId = groupService.getUserGroupId(user.getId());
        if (groupId == null) return noGroupError();

        // Return only users in the same group as potential roommates for splitting
        List<GroupMembershipEntity> memberships = membershipRepository.findByGroupId(groupId);
        List<Long> memberUserIds = memberships.stream()
                .map(GroupMembershipEntity::getUserId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(
            userRepository.findAllById(memberUserIds).stream()
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
