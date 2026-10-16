package com.moneykeeper.service;

import com.moneykeeper.dto.BudgetRequest;
import com.moneykeeper.entity.Budget;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.Transaction;
import com.moneykeeper.repository.BudgetRepository;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository, AccountBookService accountBookService,
                         AccountBookMemberService memberService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountBookService = accountBookService;
        this.memberService = memberService;
    }

    public List<Budget> getCurrentMonthBudgets(Long userId, Long accountBookId) {
        YearMonth currentMonth = YearMonth.now();
        return getBudgetsByMonth(userId, currentMonth.toString(), accountBookId);
    }

    public List<Budget> getBudgetsByMonth(Long userId, String month, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Long finalAccountBookId = accountBookId;
        List<Budget> budgets = budgetRepository.findByUserIdAndAccountBookIdAndMonth(userId, accountBookId, month);

        List<Long> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .toList();

        Map<Long, BigDecimal> usedAmounts = transactionRepository
                .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(finalAccountBookId, startDate, endDate).stream()
                .filter(t -> t.getType() == CategoryType.EXPENSE && categoryIds.contains(t.getCategoryId()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        budgets.forEach(budget -> {
            budget.setUsedAmount(usedAmounts.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO));
            budget.setCategory(categoryMap.get(budget.getCategoryId()));
        });

        return budgets;
    }

    public Budget createBudget(Long userId, BudgetRequest request) {
        Long accountBookId = request.getAccountBookId();
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        if (budgetRepository.existsByUserIdAndAccountBookIdAndMonthAndCategoryId(userId, accountBookId, request.getMonth(), request.getCategoryId())) {
            throw new IllegalArgumentException("该分类本月已设置预算");
        }

        validateCategory(accountBookId, request.getCategoryId());

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setAccountBookId(accountBookId);
        budget.setCategoryId(request.getCategoryId());
        budget.setMonth(request.getMonth());
        budget.setAmountLimit(request.getAmountLimit());
        budget.setAlertThreshold(request.getAlertThreshold() != null ? request.getAlertThreshold() : 80);

        return budgetRepository.save(budget);
    }

    public Budget updateBudget(Long userId, Long accountBookId, Long id, BudgetRequest request) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        
        checkAccess(userId, accountBookId);
        
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("预算不存在"));

        if (!existing.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权限修改此预算");
        }

        existing.setAmountLimit(request.getAmountLimit());
        if (request.getAlertThreshold() != null) {
            existing.setAlertThreshold(request.getAlertThreshold());
        }

        return budgetRepository.save(existing);
    }

    public void deleteBudget(Long userId, Long accountBookId, Long id) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        
        checkAccess(userId, accountBookId);
        
        Budget existing = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("预算不存在"));

        if (!existing.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权限删除此预算");
        }

        budgetRepository.delete(existing);
    }

    private void validateCategory(Long accountBookId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("只能为支出分类设置预算");
        }

        if (category.getAccountBookId() != null && !category.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无效的分类");
        }
    }

    private void checkAccess(Long userId, Long accountBookId) {
        if (!memberService.hasAccess(userId, accountBookId)) {
            throw new IllegalArgumentException("您没有访问此账本的权限");
        }
    }
}
