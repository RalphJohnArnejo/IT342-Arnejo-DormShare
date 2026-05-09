package edu.cit.arnejo.dormshare.expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.arnejo.dormshare.expense.entity.ExpenseEntity;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {
    List<ExpenseEntity> findByPaidByIdOrderByDateDesc(Long paidById);
    List<ExpenseEntity> findAllByOrderByDateDesc();

    // Group-scoped queries with explicit NULL handling
    @Query("SELECT e FROM ExpenseEntity e WHERE e.groupId = :groupId ORDER BY e.date DESC")
    List<ExpenseEntity> findByGroupIdOrderByDateDesc(@Param("groupId") Long groupId);
    
    @Query("SELECT e FROM ExpenseEntity e WHERE e.paidById = :paidById AND e.groupId = :groupId ORDER BY e.date DESC")
    List<ExpenseEntity> findByPaidByIdAndGroupIdOrderByDateDesc(@Param("paidById") Long paidById, @Param("groupId") Long groupId);
}
