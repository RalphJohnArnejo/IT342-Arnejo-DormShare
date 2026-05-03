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
import edu.cit.arnejo.dormshare.dto.StripePaymentConfirmRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.ExpenseService;
import edu.cit.arnejo.dormshare.service.GroupService;
import edu.cit.arnejo.dormshare.service.StripeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SettlementController {

    private final ExpenseService expenseService;
    private final GroupService groupService;
    private final StripeService stripeService;

    public SettlementController(ExpenseService expenseService, GroupService groupService, StripeService stripeService) {
        this.expenseService = expenseService;
        this.groupService = groupService;
        this.stripeService = stripeService;
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
     * Returns Stripe payment intent client secret for frontend Stripe.js integration
     */
    @PostMapping("/payments/stripe/intent")
    public ResponseEntity<ApiResponse> getStripeIntent(
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

        try {
            // Convert amount to cents for Stripe (PHP 10.00 = 1000 cents)
            Long amountInCents = Math.round(request.getAmount() * 100);
            
            java.util.Map<String, Object> paymentIntent = stripeService.createPaymentIntent(
                    amountInCents,
                    user.getId(),
                    request.getPayeeId(),
                    request.getDescription() != null ? request.getDescription() : "DormShare Payment"
            );

            return ResponseEntity.ok(ApiResponse.ok(paymentIntent));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("STRIPE-001", "Payment intent creation failed", e.getMessage()));
        }
    }

    /**
     * POST /api/payments/stripe/confirm
     * Confirms Stripe payment with payment method
     * Frontend should send paymentIntentId and paymentMethodId from Stripe.js
     */
    @PostMapping("/payments/stripe/confirm")
    public ResponseEntity<ApiResponse> confirmStripePayment(
            @RequestBody StripePaymentConfirmRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }

        if (request.getPaymentIntentId() == null || request.getPaymentIntentId().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STRIPE-002", "Missing payment intent ID", 
                            "paymentIntentId is required"));
        }

        try {
            // If paymentMethodId provided, confirm with it. Otherwise just check status.
            java.util.Map<String, Object> confirmResult;
            if (request.getPaymentMethodId() != null && !request.getPaymentMethodId().isEmpty()) {
                confirmResult = stripeService.confirmPaymentIntent(
                        request.getPaymentIntentId(),
                        request.getPaymentMethodId()
                );
            } else {
                // Just retrieve the current status
                confirmResult = stripeService.getPaymentIntentStatus(request.getPaymentIntentId());
            }

            // Check if payment was successful
            boolean isSuccessful = "succeeded".equals(confirmResult.get("status"));
            if (isSuccessful) {
                // Mark settlement as settled in database
                // TODO: Update settlement entity with status = SETTLED
                confirmResult.put("message", "Payment confirmed successfully");
            }

            return ResponseEntity.ok(ApiResponse.ok(confirmResult));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("STRIPE-003", "Payment confirmation failed", e.getMessage()));
        }
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
