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
}
