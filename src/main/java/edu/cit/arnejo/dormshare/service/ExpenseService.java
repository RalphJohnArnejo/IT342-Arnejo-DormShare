package edu.cit.arnejo.dormshare.service;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.ExpenseRequest;
import edu.cit.arnejo.dormshare.dto.ExpenseResponse;
import edu.cit.arnejo.dormshare.dto.ExpenseSummaryResponse;
import edu.cit.arnejo.dormshare.entity.ExpenseEntity;
import edu.cit.arnejo.dormshare.entity.ExpenseSplitEntity;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.repository.ExpenseRepository;
import edu.cit.arnejo.dormshare.repository.ExpenseSplitRepository;
import edu.cit.arnejo.dormshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository, 
                          ExpenseSplitRepository splitRepository, 
                          UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApiResponse createExpense(ExpenseRequest request, Long groupId) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("VALID-001", "Invalid amount", "Amount must be greater than zero");
        }
        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            return ApiResponse.error("VALID-002", "No splits", "At least one split participant is required");
        }

        // Validate payer exists
        Long paidById = request.getPaidById();
        if (paidById == null) {
            return ApiResponse.error("VALID-003", "Missing payer", "PaidById is required");
        }
        Optional<UserEntity> payer = userRepository.findById(paidById);
        if (payer.isEmpty()) {
            return ApiResponse.error("DB-001", "User not found", "Payer does not exist");
        }

        // Create main expense
        ExpenseEntity expense = new ExpenseEntity();
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setPaidById(request.getPaidById());
        expense.setCategory(request.getCategory());
        expense.setGroupId(groupId);
        
        expense = expenseRepository.save(expense);

        // Save splits
        for (ExpenseRequest.SplitRequest splitReq : request.getSplits()) {
            ExpenseSplitEntity split = new ExpenseSplitEntity();
            split.setExpenseId(expense.getId());
            split.setUserId(splitReq.getUserId());
            split.setAmountOwed(splitReq.getAmount());
            split.setIsSettled(false);
            
            // Payer's own split is marked as settled immediately (they already paid it)
            if (splitReq.getUserId().equals(request.getPaidById())) {
                split.setIsSettled(true);
            }
            
            splitRepository.save(split);
        }

        return ApiResponse.ok(mapToResponse(expense));
    }

    public ApiResponse getLedger(Long groupId) {
        List<ExpenseEntity> expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        List<ExpenseResponse> responses = expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(responses);
    }

    public ApiResponse getSummary(Long userId, Long groupId) {
        // You Owe: Sum of all splits for this user that are NOT settled
        // Only for expenses in this group
        List<ExpenseSplitEntity> youOweSplits = splitRepository.findByUserIdAndIsSettled(userId, false);
        BigDecimal totalYouOwe = BigDecimal.ZERO;
        
        for (ExpenseSplitEntity split : youOweSplits) {
            // Check if the expense belongs to this group
            Optional<ExpenseEntity> expOpt = expenseRepository.findById(split.getExpenseId());
            if (expOpt.isPresent() && groupId.equals(expOpt.get().getGroupId())) {
                totalYouOwe = totalYouOwe.add(split.getAmountOwed());
            }
        }

        // Owed to You: Sum of all splits for OTHER users for expenses paid by you in this group
        List<ExpenseEntity> yourPaidExpenses = expenseRepository.findByPaidByIdAndGroupIdOrderByDateDesc(userId, groupId);
        BigDecimal totalOwedToYou = BigDecimal.ZERO;
        
        for (ExpenseEntity exp : yourPaidExpenses) {
            List<ExpenseSplitEntity> splits = splitRepository.findByExpenseId(exp.getId());
            for (ExpenseSplitEntity split : splits) {
                // If someone else owes for your expense and hasn't settled
                if (!split.getUserId().equals(userId) && !split.getIsSettled()) {
                    totalOwedToYou = totalOwedToYou.add(split.getAmountOwed());
                }
            }
        }

        return ApiResponse.ok(new ExpenseSummaryResponse(totalOwedToYou, totalYouOwe));
    }

    @Transactional
    public ApiResponse settleSplit(Long splitId) {
        if (splitId == null) {
            return ApiResponse.error("VALID-004", "Missing split ID", "SplitId is required");
        }
        Optional<ExpenseSplitEntity> splitOpt = splitRepository.findById(splitId);
        if (splitOpt.isEmpty()) {
            return ApiResponse.error("DB-001", "Split not found", "Split ID does not exist");
        }

        ExpenseSplitEntity split = splitOpt.get();
        split.setIsSettled(true);
        splitRepository.save(split);

        return ApiResponse.ok(null);
    }

    private ExpenseResponse mapToResponse(ExpenseEntity entity) {
        ExpenseResponse res = new ExpenseResponse();
        res.setId(entity.getId());
        res.setAmount(entity.getAmount());
        res.setDescription(entity.getDescription());
        res.setPaidById(entity.getPaidById());
        res.setCategory(entity.getCategory());
        res.setDate(entity.getDate());

        // Fill payer name
        Long paidById = entity.getPaidById();
        if (paidById != null) {
            userRepository.findById(paidById).ifPresent(u -> 
                res.setPayerName(u.getFirstName() + " " + u.getLastName()));
        }

        // Fill splits
        List<ExpenseSplitEntity> splits = splitRepository.findByExpenseId(entity.getId());
        List<ExpenseResponse.SplitResponse> splitResponses = new ArrayList<>();
        
        for (ExpenseSplitEntity s : splits) {
            ExpenseResponse.SplitResponse sr = new ExpenseResponse.SplitResponse();
            sr.setId(s.getId());
            sr.setUserId(s.getUserId());
            sr.setAmountOwed(s.getAmountOwed());
            sr.setIsSettled(s.getIsSettled());
            
            Long splitUserId = s.getUserId();
            if (splitUserId != null) {
                userRepository.findById(splitUserId).ifPresent(u -> 
                    sr.setUserName(u.getFirstName() + " " + u.getLastName()));
            }
                
            splitResponses.add(sr);
        }
        res.setSplits(splitResponses);

        return res;
    }
}
