package com.moneykeeper.service;

import com.moneykeeper.entity.AccountBook;
import com.moneykeeper.entity.Budget;
import com.moneykeeper.entity.BudgetAlert;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.Transaction;
import com.moneykeeper.entity.User;
import com.moneykeeper.repository.BudgetAlertRepository;
import com.moneykeeper.repository.BudgetRepository;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TransactionRepository;
import com.moneykeeper.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetAlertService {

    private final BudgetAlertRepository budgetAlertRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AccountBookService accountBookService;

    public BudgetAlertService(BudgetAlertRepository budgetAlertRepository,
                              BudgetRepository budgetRepository,
                              TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository,
                              AccountBookService accountBookService) {
        this.budgetAlertRepository = budgetAlertRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.accountBookService = accountBookService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkBudgetAlerts() {
        List<User> users = userRepository.findAll();
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            checkUserBudgetAlerts(user.getId(), currentMonth, today);
        }
    }

    private void checkUserBudgetAlerts(Long userId, YearMonth currentMonth, LocalDate today) {
        List<AccountBook> accountBooks = accountBookService.getAccountBooks(userId);

        for (AccountBook accountBook : accountBooks) {
            checkAccountBookBudgetAlerts(userId, accountBook.getId(), currentMonth, today);
        }
    }

    private void checkAccountBookBudgetAlerts(Long userId, Long accountBookId, YearMonth currentMonth, LocalDate today) {
        List<Budget> budgets = budgetRepository.findByUserIdAndAccountBookIdAndMonth(userId, accountBookId, currentMonth.toString());

        if (budgets.isEmpty()) return;

        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        List<Long> categoryIds = budgets.stream().map(Budget::getCategoryId).toList();

        List<Transaction> transactions = transactionRepository
                .findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(accountBookId, startDate, endDate);

        Map<Long, BigDecimal> usedAmounts = transactions.stream()
                .filter(t -> t.getType() == CategoryType.EXPENSE && categoryIds.contains(t.getCategoryId()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        for (Budget budget : budgets) {
            BigDecimal usedAmount = usedAmounts.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
            int percentage = budget.getAmountLimit().compareTo(BigDecimal.ZERO) > 0
                    ? usedAmount.multiply(new BigDecimal("100"))
                            .divide(budget.getAmountLimit(), 0, RoundingMode.HALF_UP)
                            .intValue()
                    : 100;

            int threshold = budget.getAlertThreshold() != null ? budget.getAlertThreshold() : 80;

            if (percentage >= threshold) {
                boolean exists = budgetAlertRepository.existsByUserIdAndAccountBookIdAndCategoryIdAndMonthAndAlertDate(
                        userId, accountBookId, budget.getCategoryId(), budget.getMonth(), today);

                if (!exists) {
                    BudgetAlert alert = new BudgetAlert();
                    alert.setUserId(userId);
                    alert.setAccountBookId(accountBookId);
                    alert.setCategoryId(budget.getCategoryId());
                    alert.setMonth(budget.getMonth());
                    alert.setAlertDate(today);
                    alert.setIsNotified(true);
                    alert.setIsRead(false);
                    alert.setBudgetLimit(budget.getAmountLimit());
                    alert.setUsedAmount(usedAmount);
                    alert.setPercentage(percentage);

                    BigDecimal overAmount = usedAmount.subtract(budget.getAmountLimit());
                    if (overAmount.compareTo(BigDecimal.ZERO) > 0) {
                        alert.setOverAmount(overAmount);
                    }

                    budgetAlertRepository.save(alert);
                }
            }
        }
    }

    public List<BudgetAlert> getUnreadAlerts(Long userId, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        List<BudgetAlert> alerts = budgetAlertRepository.findByUserIdAndAccountBookIdAndIsReadFalseOrderByCreatedAtDesc(userId, accountBookId);
        enrichWithCategories(alerts);
        return alerts;
    }

    public List<BudgetAlert> getAllAlerts(Long userId, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        List<BudgetAlert> alerts = budgetAlertRepository.findByUserIdAndAccountBookIdOrderByCreatedAtDesc(userId, accountBookId);
        enrichWithCategories(alerts);
        return alerts;
    }

    public long getUnreadCount(Long userId, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        return budgetAlertRepository.countByUserIdAndAccountBookIdAndIsReadFalse(userId, accountBookId);
    }

    public void markAsRead(Long userId, Long accountBookId, Long alertId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        
        BudgetAlert alert = budgetAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("预警不存在"));

        if (!alert.getUserId().equals(userId) || !alert.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权限操作此预警");
        }

        alert.setIsRead(true);
        budgetAlertRepository.save(alert);
    }

    public void markAllAsRead(Long userId, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }
        List<BudgetAlert> alerts = budgetAlertRepository.findByUserIdAndAccountBookIdAndIsReadFalseOrderByCreatedAtDesc(userId, accountBookId);
        alerts.forEach(alert -> alert.setIsRead(true));
        budgetAlertRepository.saveAll(alerts);
    }

    private void enrichWithCategories(List<BudgetAlert> alerts) {
        if (alerts.isEmpty()) return;

        List<Long> categoryIds = alerts.stream()
                .map(BudgetAlert::getCategoryId)
                .distinct()
                .toList();

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        alerts.forEach(alert -> alert.setCategory(categoryMap.get(alert.getCategoryId())));
    }
}
