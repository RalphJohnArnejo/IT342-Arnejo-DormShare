package edu.cit.arnejo.dormshare.repository;

import edu.cit.arnejo.dormshare.entity.PantryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PantryItemRepository extends JpaRepository<PantryItemEntity, Long> {

    List<PantryItemEntity> findAllByOrderByCreatedAtDesc();

    List<PantryItemEntity> findByAddedByIdOrderByCreatedAtDesc(Long addedById);

    List<PantryItemEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<PantryItemEntity> findByCategoryIgnoreCaseOrderByCreatedAtDesc(String category);

    List<PantryItemEntity> findByItemNameContainingIgnoreCaseOrderByCreatedAtDesc(String itemName);

    long countByStatus(String status);

    // Group-scoped queries
    List<PantryItemEntity> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<PantryItemEntity> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, String status);

    List<PantryItemEntity> findByGroupIdAndCategoryIgnoreCaseOrderByCreatedAtDesc(Long groupId, String category);

    List<PantryItemEntity> findByGroupIdAndItemNameContainingIgnoreCaseOrderByCreatedAtDesc(Long groupId, String itemName);

    long countByGroupIdAndStatus(Long groupId, String status);

    long countByGroupId(Long groupId);
}
