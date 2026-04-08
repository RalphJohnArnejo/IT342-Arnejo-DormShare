package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.PantryItemRequest;
import edu.cit.arnejo.dormshare.entity.PantryItemEntity;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.PantryItemRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PantryService {

    private final PantryItemRepository pantryItemRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_STATUSES = Set.of("IN", "LOW", "OUT");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Dairy", "Meat", "Vegetables", "Fruits", "Snacks",
            "Beverages", "Condiments", "Grains", "Frozen", "Cleaning", "Other"
    );

    public PantryService(PantryItemRepository pantryItemRepository, UserRepository userRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all pantry items.
     */
    public ApiResponse getAllItems() {
        List<PantryItemEntity> items = pantryItemRepository.findAllByOrderByCreatedAtDesc();
        return ApiResponse.ok(items);
    }

    /**
     * Get a single pantry item by ID.
     */
    public ApiResponse getItemById(Long id) {
        Optional<PantryItemEntity> item = pantryItemRepository.findById(id);
        if (item.isEmpty()) {
            return ApiResponse.error("DB-001", "Resource not found", "Pantry item not found");
        }
        return ApiResponse.ok(item.get());
    }

    /**
     * Get items by status filter.
     */
    public ApiResponse getItemsByStatus(String status) {
        String upperStatus = status.toUpperCase();
        if (!VALID_STATUSES.contains(upperStatus)) {
            return ApiResponse.error("VALID-001", "Validation failed", "Invalid status. Must be IN, LOW, or OUT");
        }
        List<PantryItemEntity> items = pantryItemRepository.findByStatusOrderByCreatedAtDesc(upperStatus);
        return ApiResponse.ok(items);
    }

    /**
     * Get items by category filter.
     */
    public ApiResponse getItemsByCategory(String category) {
        List<PantryItemEntity> items = pantryItemRepository.findByCategoryIgnoreCaseOrderByCreatedAtDesc(category);
        return ApiResponse.ok(items);
    }

    /**
     * Search items by name.
     */
    public ApiResponse searchItems(String query) {
        List<PantryItemEntity> items = pantryItemRepository.findByItemNameContainingIgnoreCaseOrderByCreatedAtDesc(query);
        return ApiResponse.ok(items);
    }

    /**
     * Get pantry stats (counts by status).
     */
    public ApiResponse getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", pantryItemRepository.count());
        stats.put("inStock", pantryItemRepository.countByStatus("IN"));
        stats.put("lowStock", pantryItemRepository.countByStatus("LOW"));
        stats.put("outOfStock", pantryItemRepository.countByStatus("OUT"));
        return ApiResponse.ok(stats);
    }

    /**
     * Add a new pantry item.
     */
    public ApiResponse addItem(PantryItemRequest request, Long userId) {
        // Validate item name
        if (request.getItemName() == null || request.getItemName().trim().isEmpty()) {
            return ApiResponse.error("VALID-001", "Validation failed", "Item name is required");
        }

        // Validate status
        String status = (request.getStatus() != null) ? request.getStatus().toUpperCase() : "IN";
        if (!VALID_STATUSES.contains(status)) {
            return ApiResponse.error("VALID-001", "Validation failed", "Status must be IN, LOW, or OUT");
        }

        // Validate category
        String category = (request.getCategory() != null) ? request.getCategory() : "Other";

        // Validate quantity
        Double quantity = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1.0;

        // Get user info
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        String userName = "Unknown";
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            userName = user.getFirstName() + " " + user.getLastName();
        }

        PantryItemEntity item = new PantryItemEntity();
        item.setItemName(request.getItemName().trim());
        item.setCategory(category);
        item.setStatus(status);
        item.setQuantity(quantity);
        item.setAddedById(userId);
        item.setAddedByName(userName);
        item.setUpdatedById(userId);
        item.setUpdatedByName(userName);

        PantryItemEntity saved = pantryItemRepository.save(item);
        return ApiResponse.ok(saved);
    }

    /**
     * Update a pantry item (status, quantity, category, name).
     */
    public ApiResponse updateItem(Long itemId, PantryItemRequest request, Long userId) {
        Optional<PantryItemEntity> itemOpt = pantryItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return ApiResponse.error("DB-001", "Resource not found", "Pantry item not found");
        }

        PantryItemEntity item = itemOpt.get();

        // Get user info
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        String userName = "Unknown";
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            userName = user.getFirstName() + " " + user.getLastName();
        }

        // Update fields if provided
        if (request.getItemName() != null && !request.getItemName().trim().isEmpty()) {
            item.setItemName(request.getItemName().trim());
        }

        if (request.getStatus() != null) {
            String status = request.getStatus().toUpperCase();
            if (!VALID_STATUSES.contains(status)) {
                return ApiResponse.error("VALID-001", "Validation failed", "Status must be IN, LOW, or OUT");
            }
            item.setStatus(status);
        }

        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }

        if (request.getQuantity() != null && request.getQuantity() > 0) {
            item.setQuantity(request.getQuantity());
        }

        item.setUpdatedById(userId);
        item.setUpdatedByName(userName);

        PantryItemEntity saved = pantryItemRepository.save(item);
        return ApiResponse.ok(saved);
    }

    /**
     * Delete a pantry item.
     */
    public ApiResponse deleteItem(Long itemId, Long userId) {
        Optional<PantryItemEntity> itemOpt = pantryItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return ApiResponse.error("DB-001", "Resource not found", "Pantry item not found");
        }

        pantryItemRepository.deleteById(itemId);
        return ApiResponse.ok("Pantry item deleted successfully");
    }
}
