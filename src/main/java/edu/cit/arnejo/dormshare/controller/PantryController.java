package edu.cit.arnejo.dormshare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.PantryItemRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.GroupService;
import edu.cit.arnejo.dormshare.service.PantryService;

@RestController
@RequestMapping("/api/pantry")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PantryController {

    private final PantryService pantryService;
    private final GroupService groupService;

    public PantryController(PantryService pantryService, GroupService groupService) {
        this.pantryService = pantryService;
        this.groupService = groupService;
    }

    /**
     * Helper: resolve user's group ID, return null if not in a group.
     */
    private Long resolveGroupId(UserEntity user) {
        return groupService.getUserGroupId(user.getId());
    }

    /**
     * Helper: return an error response when user has no group.
     */
    private ResponseEntity<ApiResponse> noGroupError() {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("GROUP-003", "Not in a group",
                        "You must join or create a group before using the pantry"));
    }

    /**
     * GET /api/pantry?groupId=...
     * Retrieves all pantry items for a group. If groupId is not provided, uses user's primary group.
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllItems(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long verifiedGroupId = groupService.getVerifiedGroupId(user.getId(), groupId);
        if (verifiedGroupId == null) return noGroupError();

        ApiResponse result = pantryService.getAllItems(verifiedGroupId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/pantry/stats?groupId=...
     * Get pantry statistics (counts by status) for a group. If groupId is not provided, uses user's primary group.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getStats(
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long verifiedGroupId = groupService.getVerifiedGroupId(user.getId(), groupId);
        if (verifiedGroupId == null) return noGroupError();

        ApiResponse result = pantryService.getStats(verifiedGroupId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/pantry/{id}
     * Retrieves a single pantry item.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getItemById(@PathVariable Long id) {
        ApiResponse result = pantryService.getItemById(id);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * GET /api/pantry/status/{status}?groupId=...
     * Filter items by status (IN, LOW, OUT) within a group.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getItemsByStatus(
            @PathVariable String status,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long verifiedGroupId = groupService.getVerifiedGroupId(user.getId(), groupId);
        if (verifiedGroupId == null) return noGroupError();

        ApiResponse result = pantryService.getItemsByStatus(verifiedGroupId, status);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * GET /api/pantry/category/{category}?groupId=...
     * Filter items by category within a group.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getItemsByCategory(
            @PathVariable String category,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long verifiedGroupId = groupService.getVerifiedGroupId(user.getId(), groupId);
        if (verifiedGroupId == null) return noGroupError();

        ApiResponse result = pantryService.getItemsByCategory(verifiedGroupId, category);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/pantry/search?q=...&groupId=...
     * Search items by name within a group.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchItems(
            @RequestParam String q,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long verifiedGroupId = groupService.getVerifiedGroupId(user.getId(), groupId);
        if (verifiedGroupId == null) return noGroupError();

        ApiResponse result = pantryService.searchItems(verifiedGroupId, q);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/pantry
     * Adds a new item to the user's group pantry.
     */
    @PostMapping
    public ResponseEntity<ApiResponse> addItem(
            @RequestBody PantryItemRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        Long groupId = resolveGroupId(user);
        if (groupId == null) return noGroupError();

        ApiResponse result = pantryService.addItem(request, user.getId(), groupId);
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * PATCH /api/pantry/{itemId}
     * Updates a pantry item (status, quantity, name, category).
     */
    @PatchMapping("/{itemId}")
    public ResponseEntity<ApiResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody PantryItemRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = pantryService.updateItem(itemId, request, user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        if (result.getError() != null && "DB-001".equals(result.getError().getCode())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * DELETE /api/pantry/{itemId}
     * Removes a pantry item.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = pantryService.deleteItem(itemId, user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
