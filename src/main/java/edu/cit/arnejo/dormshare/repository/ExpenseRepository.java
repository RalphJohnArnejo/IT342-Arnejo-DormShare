package edu.cit.arnejo.dormshare.repository;

import edu.cit.arnejo.dormshare.entity.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {
    List<ExpenseEntity> findByPaidByIdOrderByDateDesc(Long paidById);
    List<ExpenseEntity> findAllByOrderByDateDesc();

    // Group-scoped queries
    List<ExpenseEntity> findByGroupIdOrderByDateDesc(Long groupId);
    List<ExpenseEntity> findByPaidByIdAndGroupIdOrderByDateDesc(Long paidById, Long groupId);
}
