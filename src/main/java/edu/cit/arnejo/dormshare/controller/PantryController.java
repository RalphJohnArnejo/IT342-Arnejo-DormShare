package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.PantryItemRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.PantryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pantry")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PantryController {

    private final PantryService pantryService;

    public PantryController(PantryService pantryService) {
        this.pantryService = pantryService;
    }

    /**
     * GET /api/pantry
     * Retrieves all pantry items.
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllItems() {
        ApiResponse result = pantryService.getAllItems();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/pantry/stats
     * Get pantry statistics (counts by status).
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getStats() {
        ApiResponse result = pantryService.getStats();
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
     * GET /api/pantry/status/{status}
     * Filter items by status (IN, LOW, OUT).
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getItemsByStatus(@PathVariable String status) {
        ApiResponse result = pantryService.getItemsByStatus(status);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * GET /api/pantry/category/{category}
     * Filter items by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getItemsByCategory(@PathVariable String category) {
        ApiResponse result = pantryService.getItemsByCategory(category);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/pantry/search?q=...
     * Search items by name.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchItems(@RequestParam String q) {
        ApiResponse result = pantryService.searchItems(q);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/pantry
     * Adds a new item to the pantry.
     */
    @PostMapping
    public ResponseEntity<ApiResponse> addItem(
            @RequestBody PantryItemRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = pantryService.addItem(request, user.getId());
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
