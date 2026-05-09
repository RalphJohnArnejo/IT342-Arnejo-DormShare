package edu.cit.arnejo.dormshare.expense;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.expense.dto.ExpenseRequest;
import edu.cit.arnejo.dormshare.group.entity.GroupMembershipEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.group.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;
import edu.cit.arnejo.dormshare.expense.ExpenseService;
import edu.cit.arnejo.dormshare.group.GroupService;

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

    /**
     * Helper: validate that user is a member of the requested group
     */
    private boolean isUserInGroup(Long userId, Long groupId) {
        List<GroupMembershipEntity> memberships = membershipRepository.findByGroupId(groupId);
        return memberships.stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> logExpense(
            @RequestBody ExpenseRequest request,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        
        // Use provided groupId, otherwise get user's primary group
        if (groupId == null) {
            groupId = groupService.getUserGroupId(user.getId());
        }
        if (groupId == null) return noGroupError();

        // Validate user is a member of the group
        if (!isUserInGroup(user.getId(), groupId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("AUTH-003", "Access denied", "You are not a member of this group"));
        }

        // Force the paidById to be the current user for security
        request.setPaidById(user.getId());
        
        ApiResponse result = expenseService.createExpense(request, groupId);
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse> getLedger(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        
        // Use provided groupId, otherwise get user's primary group
        if (groupId == null) {
            groupId = groupService.getUserGroupId(user.getId());
        }
        if (groupId == null) return noGroupError();

        // Validate user is a member of the group
        if (!isUserInGroup(user.getId(), groupId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("AUTH-003", "Access denied", "You are not a member of this group"));
        }

        ApiResponse result = expenseService.getLedger(groupId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getSummary(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        
        // Use provided groupId, otherwise get user's primary group
        if (groupId == null) {
            groupId = groupService.getUserGroupId(user.getId());
        }
        if (groupId == null) return noGroupError();

        // Validate user is a member of the group
        if (!isUserInGroup(user.getId(), groupId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("AUTH-003", "Access denied", "You are not a member of this group"));
        }

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
        ApiResponse result = expenseService.settleSplit(splitId, user.getId());
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
