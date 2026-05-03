package edu.cit.arnejo.dormshare.controller;

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

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.PaymentInitiateRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.ExpenseService;
import edu.cit.arnejo.dormshare.service.GroupService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SettlementController {

    private final ExpenseService expenseService;
    private final GroupService groupService;

    public SettlementController(ExpenseService expenseService, GroupService groupService) {
        this.expenseService = expenseService;
        this.groupService = groupService;
    }

    /**
     * GET /api/ledger/summary
     * Returns settlement summary showing who owes whom
     */
    @GetMapping("/ledger/summary")
    public ResponseEntity<ApiResponse> getLedgerSummary(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        // If no groupId provided, use user's default group
        if (groupId == null) {
            groupId = groupService.getUserGroupId(user.getId());
        }

        if (groupId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP-003", "Not in a group",
                            "You must join or create a group to view settlements"));
        }

        ApiResponse result = expenseService.getLedgerSummary(groupId, user.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ledger/history
     * Returns settlement history (paid transactions)
     */
    @GetMapping("/ledger/history")
    public ResponseEntity<ApiResponse> getLedgerHistory(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (groupId == null) {
            groupId = groupService.getUserGroupId(user.getId());
        }

        if (groupId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP-003", "Not in a group",
                            "You must join or create a group to view settlement history"));
        }

        ApiResponse result = expenseService.getLedgerHistory(groupId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/payments/initiate
     * Initiates a payment between users
     */
    @PostMapping("/payments/initiate")
    public ResponseEntity<ApiResponse> initiatePayment(
            @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALID-001", "Invalid amount", "Amount must be greater than 0"));
        }

        ApiResponse result = expenseService.initiatePayment(user.getId(), request.getPayeeId(), 
                request.getAmount(), request.getGroupId());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/payments/stripe/intent
     * Returns Stripe payment intent client secret
     */
    @PostMapping("/payments/stripe/intent")
    public ResponseEntity<ApiResponse> getStripeIntent(
            @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        // For now, return a mock response. In production, integrate with Stripe API
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("clientSecret", "pi_mock_" + System.currentTimeMillis());
        data.put("amount", request.getAmount());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * POST /api/payments/stripe/confirm
     * Confirms Stripe payment
     */
    @PostMapping("/payments/stripe/confirm")
    public ResponseEntity<ApiResponse> confirmStripePayment(
            @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        // For now, mark settlement as settled
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("status", "SETTLED");
        data.put("message", "Payment confirmed successfully");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * POST /api/settle/proof/{settlementId}
     * Uploads payment proof (for manual verification)
     */
    @PostMapping("/settle/proof/{settlementId}")
    public ResponseEntity<ApiResponse> uploadPaymentProof(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("proofId", "proof_" + settlementId);
        data.put("status", "PENDING_VERIFICATION");
        data.put("message", "Payment proof uploaded. Awaiting verification.");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * PATCH /api/settle/verify/{settlementId}
     * Verifies payment proof (admin only)
     */
    @PatchMapping("/settle/verify/{settlementId}")
    public ResponseEntity<ApiResponse> verifyPaymentProof(
            @PathVariable Long settlementId,
            @RequestParam String action,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (!user.getRole().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("PERM-001", "Permission denied", "Only admins can verify payments"));
        }

        String status = action.equalsIgnoreCase("approve") ? "SETTLED" : "REJECTED";
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("settlementId", settlementId);
        data.put("status", status);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
