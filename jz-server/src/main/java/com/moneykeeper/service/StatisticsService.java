package com.moneykeeper.service;

import com.moneykeeper.dto.CategoryTrendData;
import com.moneykeeper.dto.CurvePoint;
import com.moneykeeper.dto.MonthlyStatistics;
import com.moneykeeper.dto.YearlyStatistics;
import com.moneykeeper.dto.TrendData;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;

    public StatisticsService(TransactionService transactionService,
                            CategoryRepository categoryRepository,
                            TransactionRepository transactionRepository,
                            AccountBookService accountBookService,
                            AccountBookMemberService memberService) {
        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.accountBookService = accountBookService;
        this.memberService = memberService;
    }

    public MonthlyStatistics getMonthlyStatistics(Long userId, Long accountBookId, String month) {
        checkAccess(userId, accountBookId);
        YearMonth yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal totalExpense = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        BigDecimal totalIncome = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.INCOME, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Map<String, Object>> dailyTrend = buildDailyTrend(userId, accountBookId, startDate, endDate);
        List<Map<String, Object>> expenseByCategory = buildCategoryStats(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        List<Map<String, Object>> incomeByCategory = buildCategoryStats(userId, accountBookId, CategoryType.INCOME, startDate, endDate);

        return new MonthlyStatistics(totalExpense, totalIncome, balance, dailyTrend, expenseByCategory, incomeByCategory);
    }

    public YearlyStatistics getYearlyStatistics(Long userId, Long accountBookId, Integer year) {
        checkAccess(userId, accountBookId);
        int targetYear = year != null ? year : LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = LocalDate.of(targetYear, 12, 31);

        BigDecimal totalExpense = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        BigDecimal totalIncome = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.INCOME, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Map<String, Object>> monthlyTrend = buildMonthlyTrend(userId, accountBookId, startDate, endDate);
        List<Map<String, Object>> expenseByCategory = buildCategoryStats(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        List<Map<String, Object>> incomeByCategory = buildCategoryStats(userId, accountBookId, CategoryType.INCOME, startDate, endDate);

        return new YearlyStatistics(totalExpense, totalIncome, balance, monthlyTrend, expenseByCategory, incomeByCategory);
    }

    /**
     * 全部统计：有史以来所有数据。
     * 优化：总额和分类统计用宽日期范围的 SQL 聚合查询；趋势数据用按月聚合查询（非按天），
     * 避免多年数据按天展开导致内存溢出。
     */
    public YearlyStatistics getAllTimeStatistics(Long userId, Long accountBookId) {
        checkAccess(userId, accountBookId);
        LocalDate startDate = LocalDate.of(1970, 1, 1);
        LocalDate endDate = LocalDate.now();

        BigDecimal totalExpense = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        BigDecimal totalIncome = transactionService.getMonthlyTotal(userId, accountBookId, CategoryType.INCOME, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Map<String, Object>> monthlyTrend = buildMonthlyTrendFromMonthlyAgg(userId, accountBookId, startDate, endDate);
        List<Map<String, Object>> expenseByCategory = buildCategoryStats(userId, accountBookId, CategoryType.EXPENSE, startDate, endDate);
        List<Map<String, Object>> incomeByCategory = buildCategoryStats(userId, accountBookId, CategoryType.INCOME, startDate, endDate);

        return new YearlyStatistics(totalExpense, totalIncome, balance, monthlyTrend, expenseByCategory, incomeByCategory);
    }

    public List<CurvePoint> getCurveData(Long userId, Long accountBookId, String mode, String month, Integer year) {
        checkAccess(userId, accountBookId);
        if ("year".equals(mode)) {
            return getYearlyCurveData(userId, accountBookId, year);
        } else if ("all".equals(mode)) {
            return getAllTimeCurveData(userId, accountBookId);
        } else {
            return getMonthlyCurveData(userId, accountBookId, month);
        }
    }

    private List<CurvePoint> getMonthlyCurveData(Long userId, Long accountBookId, String month) {
        YearMonth yearMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Object[]> rawData = transactionService.getDailyTrend(userId, accountBookId, startDate, endDate);

        Map<LocalDate, Map<String, BigDecimal>> dateMap = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            LocalDate date = (LocalDate) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            dateMap.computeIfAbsent(date, k -> new HashMap<>())
                    .put(type.name(), amount);
        }

        List<CurvePoint> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, BigDecimal> amounts = dateMap.getOrDefault(current, new HashMap<>());
            result.add(new CurvePoint(
                    String.valueOf(current.getDayOfMonth()),
                    amounts.getOrDefault("INCOME", BigDecimal.ZERO),
                    amounts.getOrDefault("EXPENSE", BigDecimal.ZERO)
            ));
            current = current.plusDays(1);
        }

        return result;
    }

    private List<CurvePoint> getYearlyCurveData(Long userId, Long accountBookId, Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = LocalDate.of(targetYear, 12, 31);

        List<Object[]> rawData = transactionService.getDailyTrend(userId, accountBookId, startDate, endDate);

        Map<Integer, Map<String, BigDecimal>> monthMap = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            LocalDate date = (LocalDate) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            int m = date.getMonthValue();
            monthMap.computeIfAbsent(m, k -> new HashMap<>());
            if (type == CategoryType.INCOME) {
                monthMap.get(m).merge("INCOME", amount, BigDecimal::add);
            } else {
                monthMap.get(m).merge("EXPENSE", amount, BigDecimal::add);
            }
        }

        List<CurvePoint> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, BigDecimal> amounts = monthMap.getOrDefault(m, new HashMap<>());
            result.add(new CurvePoint(
                    m + "月",
                    amounts.getOrDefault("INCOME", BigDecimal.ZERO),
                    amounts.getOrDefault("EXPENSE", BigDecimal.ZERO)
            ));
        }

        return result;
    }

    /**
     * 全部曲线数据：使用按月聚合查询（非按天），聚合为年度曲线点。
     * 10年数据仅产生~120行月度聚合行，再聚合为~10个年度点，内存安全。
     */
    private List<CurvePoint> getAllTimeCurveData(Long userId, Long accountBookId) {
        LocalDate startDate = LocalDate.of(1970, 1, 1);
        List<Object[]> rawData = transactionService.getMonthlyTrendByAccountBookId(userId, accountBookId, startDate);

        Map<Integer, Map<String, BigDecimal>> yearMap = new TreeMap<>();
        for (Object[] row : rawData) {
            String monthStr = (String) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            int year = Integer.parseInt(monthStr.substring(0, 4));
            yearMap.computeIfAbsent(year, k -> new HashMap<>());
            if (type == CategoryType.INCOME) {
                yearMap.get(year).merge("INCOME", amount, BigDecimal::add);
            } else {
                yearMap.get(year).merge("EXPENSE", amount, BigDecimal::add);
            }
        }

        List<CurvePoint> result = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, BigDecimal>> entry : yearMap.entrySet()) {
            Map<String, BigDecimal> amounts = entry.getValue();
            result.add(new CurvePoint(
                    String.valueOf(entry.getKey()),
                    amounts.getOrDefault("INCOME", BigDecimal.ZERO),
                    amounts.getOrDefault("EXPENSE", BigDecimal.ZERO)
            ));
        }

        return result;
    }

    /**
     * 从月度聚合查询构建月度趋势（非按天查询），用于全部模式的趋势数据。
     */
    private List<Map<String, Object>> buildMonthlyTrendFromMonthlyAgg(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = transactionService.getMonthlyTrendByAccountBookId(userId, accountBookId, startDate);

        Map<String, Map<String, BigDecimal>> monthMap = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String m = (String) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            monthMap.computeIfAbsent(m, k -> new HashMap<>());
            if (type == CategoryType.INCOME) {
                monthMap.get(m).merge("INCOME", amount, BigDecimal::add);
            } else {
                monthMap.get(m).merge("EXPENSE", amount, BigDecimal::add);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : monthMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", entry.getKey());
            Map<String, BigDecimal> amounts = entry.getValue();
            item.put("expense", amounts.getOrDefault("EXPENSE", BigDecimal.ZERO));
            item.put("income", amounts.getOrDefault("INCOME", BigDecimal.ZERO));
            result.add(item);
        }

        return result;
    }

    public List<TrendData> getMonthlyTrend(Long userId, Long accountBookId, int months) {
        checkAccess(userId, accountBookId);
        LocalDate startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);

        List<Object[]> rawData;
        if (accountBookId != null) {
            rawData = transactionService.getMonthlyTrendByAccountBookId(userId, accountBookId, startDate);
        } else {
            rawData = transactionRepository.getMonthlyTrend(userId, startDate);
        }

        Map<String, BigDecimal> incomeMap = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            String m = (String) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            if (type == CategoryType.INCOME) {
                incomeMap.put(m, amount);
            } else {
                expenseMap.put(m, amount);
            }
        }

        List<TrendData> result = new ArrayList<>();
        LocalDate current = startDate;
        for (int i = 0; i < months; i++) {
            String monthKey = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            result.add(new TrendData(
                    monthKey,
                    incomeMap.getOrDefault(monthKey, BigDecimal.ZERO),
                    expenseMap.getOrDefault(monthKey, BigDecimal.ZERO)
            ));
            current = current.plusMonths(1);
        }

        return result;
    }

    public List<CategoryTrendData> getCategoryTrend(Long userId, Long accountBookId, Long categoryId, int months) {
        checkAccess(userId, accountBookId);
        LocalDate startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);

        List<Object[]> rawData;
        if (accountBookId != null) {
            rawData = transactionService.getCategoryMonthlyTrendByAccountBookId(userId, accountBookId, categoryId, startDate);
        } else {
            rawData = transactionRepository.getCategoryMonthlyTrend(userId, categoryId, startDate);
        }

        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String m = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            amountMap.put(m, amount);
        }

        List<CategoryTrendData> result = new ArrayList<>();
        LocalDate current = startDate;
        for (int i = 0; i < months; i++) {
            String monthKey = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            result.add(new CategoryTrendData(
                    monthKey,
                    amountMap.getOrDefault(monthKey, BigDecimal.ZERO)
            ));
            current = current.plusMonths(1);
        }

        return result;
    }

    private List<Map<String, Object>> buildDailyTrend(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = transactionService.getDailyTrend(userId, accountBookId, startDate, endDate);

        Map<LocalDate, Map<String, BigDecimal>> dateMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            LocalDate date = (LocalDate) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            dateMap.computeIfAbsent(date, k -> new HashMap<>())
                    .put(type.name(), amount);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", current.toString());
            Map<String, BigDecimal> amounts = dateMap.getOrDefault(current, new HashMap<>());
            item.put("expense", amounts.getOrDefault("EXPENSE", BigDecimal.ZERO));
            item.put("income", amounts.getOrDefault("INCOME", BigDecimal.ZERO));
            result.add(item);
            current = current.plusDays(1);
        }

        return result;
    }

    private List<Map<String, Object>> buildMonthlyTrend(Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = transactionService.getDailyTrend(userId, accountBookId, startDate, endDate);

        Map<String, Map<String, BigDecimal>> monthMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            LocalDate date = (LocalDate) row[0];
            CategoryType type = (CategoryType) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            monthMap.computeIfAbsent(monthKey, k -> new HashMap<>());
            if (type == CategoryType.INCOME) {
                monthMap.get(monthKey).merge("INCOME", amount, BigDecimal::add);
            } else {
                monthMap.get(monthKey).merge("EXPENSE", amount, BigDecimal::add);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);
        while (!current.isAfter(endYm)) {
            String monthKey = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthKey);
            Map<String, BigDecimal> amounts = monthMap.getOrDefault(monthKey, new HashMap<>());
            item.put("expense", amounts.getOrDefault("EXPENSE", BigDecimal.ZERO));
            item.put("income", amounts.getOrDefault("INCOME", BigDecimal.ZERO));
            result.add(item);
            current = current.plusMonths(1);
        }

        return result;
    }

    private List<Map<String, Object>> buildCategoryStats(Long userId, Long accountBookId, CategoryType type, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = transactionService.getMonthlyTotalByCategory(userId, accountBookId, type, startDate, endDate);

        if (rawData.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> categoryIds = rawData.stream()
                .map(row -> {
                    Object id = row[0];
                    if (id instanceof Long) return (Long) id;
                    if (id instanceof Integer) return ((Integer) id).longValue();
                    if (id instanceof Number) return ((Number) id).longValue();
                    return Long.valueOf(id.toString());
                })
                .toList();

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        BigDecimal total = rawData.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rawData) {
            Object idObj = row[0];
            Long categoryId;
            if (idObj instanceof Long) categoryId = (Long) idObj;
            else if (idObj instanceof Integer) categoryId = ((Integer) idObj).longValue();
            else if (idObj instanceof Number) categoryId = ((Number) idObj).longValue();
            else categoryId = Long.valueOf(idObj.toString());

            BigDecimal amount = (BigDecimal) row[1];
            Category category = categoryMap.get(categoryId);

            String categoryName = "未分类";
            String categoryIcon = "📌";
            if (category != null) {
                categoryName = category.getName();
                categoryIcon = category.getIcon();
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", categoryId);
            item.put("categoryName", categoryName);
            item.put("categoryIcon", categoryIcon);
            item.put("amount", amount);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                item.put("percentage", amount.multiply(new BigDecimal("100")).divide(total, 2, java.math.RoundingMode.HALF_UP));
            } else {
                item.put("percentage", BigDecimal.ZERO);
            }
            result.add(item);
        }

        result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return result;
    }

    private void checkAccess(Long userId, Long accountBookId) {
        if (accountBookId != null && !memberService.hasAccess(userId, accountBookId)) {
            throw new IllegalArgumentException("您没有访问此账本的权限");
        }
    }
}
