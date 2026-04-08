package edu.cit.arnejo.dormshare.repository;

import edu.cit.arnejo.dormshare.entity.ExpenseSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplitEntity, Long> {
    List<ExpenseSplitEntity> findByExpenseId(Long expenseId);
    List<ExpenseSplitEntity> findByUserIdAndIsSettled(Long userId, Boolean isSettled);
    
    // Splits where other people owe the user (user is the payer)
    // We'll need a join or a service-side calculation for this if we don't store paid_by_id in split
    // For simplicity, we'll use paid_by_id from the Expense entity in the service logic.
}
