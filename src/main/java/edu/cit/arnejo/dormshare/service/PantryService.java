package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.PantryItemRequest;
import edu.cit.arnejo.dormshare.entity.PantryItemEntity;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.entity.GroupMembershipEntity;
import edu.cit.arnejo.dormshare.repository.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.repository.PantryItemRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PantryService {

    private final PantryItemRepository pantryItemRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository membershipRepository;
    private final NotificationService notificationService;

    private static final Set<String> VALID_STATUSES = Set.of("IN", "LOW", "OUT");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Dairy", "Meat", "Vegetables", "Fruits", "Snacks",
            "Beverages", "Condiments", "Grains", "Frozen", "Cleaning", "Other"
    );

    public PantryService(PantryItemRepository pantryItemRepository,
                         UserRepository userRepository,
                         GroupMembershipRepository membershipRepository,
                         NotificationService notificationService) {
        this.pantryItemRepository = pantryItemRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    private void notifyGroupExceptActor(Long groupId, Long actorUserId, String type, String title, String body) {
        if (groupId == null) return;
        List<GroupMembershipEntity> memberships = membershipRepository.findByGroupId(groupId);
        List<Long> userIds = memberships.stream().map(GroupMembershipEntity::getUserId).toList();
        notificationService.createForUsers(userIds, actorUserId, type, title, body);
    }

    /**
     * Get all pantry items for a group.
     */
    public ApiResponse getAllItems(Long groupId) {
        List<PantryItemEntity> items = pantryItemRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
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
     * Get items by status filter within a group.
     */
    public ApiResponse getItemsByStatus(Long groupId, String status) {
        String upperStatus = status.toUpperCase();
        if (!VALID_STATUSES.contains(upperStatus)) {
            return ApiResponse.error("VALID-001", "Validation failed", "Invalid status. Must be IN, LOW, or OUT");
        }
        List<PantryItemEntity> items = pantryItemRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, upperStatus);
        return ApiResponse.ok(items);
    }

    /**
     * Get items by category filter within a group.
     */
    public ApiResponse getItemsByCategory(Long groupId, String category) {
        List<PantryItemEntity> items = pantryItemRepository.findByGroupIdAndCategoryIgnoreCaseOrderByCreatedAtDesc(groupId, category);
        return ApiResponse.ok(items);
    }

    /**
     * Search items by name within a group.
     */
    public ApiResponse searchItems(Long groupId, String query) {
        List<PantryItemEntity> items = pantryItemRepository.findByGroupIdAndItemNameContainingIgnoreCaseOrderByCreatedAtDesc(groupId, query);
        return ApiResponse.ok(items);
    }

    /**
     * Get pantry stats (counts by status) for a group.
     */
    public ApiResponse getStats(Long groupId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", pantryItemRepository.countByGroupId(groupId));
        stats.put("inStock", pantryItemRepository.countByGroupIdAndStatus(groupId, "IN"));
        stats.put("lowStock", pantryItemRepository.countByGroupIdAndStatus(groupId, "LOW"));
        stats.put("outOfStock", pantryItemRepository.countByGroupIdAndStatus(groupId, "OUT"));
        return ApiResponse.ok(stats);
    }

    /**
     * Add a new pantry item to a group.
     */
    public ApiResponse addItem(PantryItemRequest request, Long userId, Long groupId) {
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
        item.setGroupId(groupId);
        item.setAddedById(userId);
        item.setAddedByName(userName);
        item.setUpdatedById(userId);
        item.setUpdatedByName(userName);

        PantryItemEntity saved = pantryItemRepository.save(item);

        notifyGroupExceptActor(
            groupId,
            userId,
            "PANTRY_UPDATED",
            "Pantry item added",
            userName + " added \"" + saved.getItemName() + "\" (" + saved.getStatus() + ")"
        );

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

        notifyGroupExceptActor(
            saved.getGroupId(),
            userId,
            "PANTRY_UPDATED",
            "Pantry item updated",
            userName + " updated \"" + saved.getItemName() + "\" (" + saved.getStatus() + ")"
        );

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

        PantryItemEntity existing = itemOpt.get();
        pantryItemRepository.deleteById(itemId);

        notifyGroupExceptActor(
            existing.getGroupId(),
            userId,
            "PANTRY_UPDATED",
            "Pantry item removed",
            "A pantry item was removed: \"" + existing.getItemName() + "\""
        );
        return ApiResponse.ok("Pantry item deleted successfully");
    }
}
