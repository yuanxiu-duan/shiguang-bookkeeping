package com.moneykeeper.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.moneykeeper.dto.Result;
import com.moneykeeper.dto.TransactionExportRequest;
import com.moneykeeper.dto.TransactionRequest;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Transaction;
import com.moneykeeper.entity.TransactionExportTask;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.ClassificationService;
import com.moneykeeper.service.ExcelService;
import com.moneykeeper.service.TransactionExportTaskService;
import com.moneykeeper.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ExcelService excelService;
    private final ClassificationService classificationService;
    private final TransactionExportTaskService exportTaskService;

    public TransactionController(TransactionService transactionService,
                                 ExcelService excelService,
                                 ClassificationService classificationService,
                                 TransactionExportTaskService exportTaskService) {
        this.transactionService = transactionService;
        this.excelService = excelService;
        this.classificationService = classificationService;
        this.exportTaskService = exportTaskService;
    }

    @GetMapping
    public Result<Page<Transaction>> getTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (month != null && !month.isEmpty()) {
            YearMonth ym = YearMonth.parse(month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else if (year != null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        Pageable pageable = PageRequest.of(page, size);

        // 解析 type：ALL 或空表示不过滤
        Category.CategoryType categoryType = null;
        if (type != null && !type.isEmpty() && !type.equals("ALL")) {
            categoryType = Category.CategoryType.valueOf(type);
        }

        Page<Transaction> result = transactionService.getTransactions(
                user.getId(), startDate, endDate, tagId, accountBookId, categoryType, pageable);

        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<Page<Transaction>> searchTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String categoryIds,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Category.CategoryType categoryType = null;
        if (type != null && !type.isEmpty() && !type.equals("ALL")) {
            categoryType = Category.CategoryType.valueOf(type);
        }

        List<Long> categoryIdList = null;
        if (categoryIds != null && !categoryIds.trim().isEmpty()) {
            categoryIdList = Arrays.stream(categoryIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));

        Page<Transaction> result = transactionService.searchTransactions(
                user.getId(), accountBookId, startDate, endDate, categoryType,
                categoryIdList, account, minAmount, maxAmount, keyword, pageable);

        return Result.success(result);
    }

    @GetMapping("/all")
    public Result<Page<Transaction>> getAllTransactionsPaged(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Category.CategoryType categoryType = null;
        if (type != null && !type.isEmpty() && !type.equals("ALL")) {
            categoryType = Category.CategoryType.valueOf(type);
        }
        
        Page<Transaction> result = transactionService.getAllTransactionsPaged(user.getId(), accountBookId, categoryType, pageable);
        return Result.success(result);
    }
    
    @GetMapping("/monthly-summary")
    public Result<Map<String, Object>> getMonthlySummary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long accountBookId) {
        
        LocalDate startDate, endDate;
        
        if (month != null && !month.isEmpty()) {
            YearMonth ym = YearMonth.parse(month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else if (year != null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }
        
        BigDecimal income = transactionService.getMonthlyTotal(user.getId(), accountBookId, Category.CategoryType.INCOME, startDate, endDate);
        BigDecimal expense = transactionService.getMonthlyTotal(user.getId(), accountBookId, Category.CategoryType.EXPENSE, startDate, endDate);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("income", income != null ? income : BigDecimal.ZERO);
        summary.put("expense", expense != null ? expense : BigDecimal.ZERO);
        summary.put("balance", (income != null ? income : BigDecimal.ZERO).subtract(expense != null ? expense : BigDecimal.ZERO));
        
        return Result.success(summary);
    }

    @GetMapping("/recent")
    public Result<List<Transaction>> getRecentTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(transactionService.getRecentTransactions(user.getId(), days, accountBookId));
    }

    @PostMapping
    public Result<Transaction> createTransaction(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransactionRequest request) {
        return Result.success(transactionService.createTransaction(user.getId(), request));
    }

    @PutMapping("/{id}")
    public Result<Transaction> updateTransaction(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id,
            @RequestBody TransactionRequest request) {
        return Result.success(transactionService.updateTransaction(user.getId(), accountBookId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTransaction(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id) {
        transactionService.deleteTransaction(user.getId(), accountBookId, id);
        return Result.success(null);
    }

    @PostMapping("/classify-suggest")
    public Result<Map<String, Object>> classifySuggest(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestBody Map<String, String> body) {
        String note = body.get("note");
        ClassificationService.ClassificationSuggestion suggestion = classificationService.suggestCategory(user.getId(), accountBookId, note);

        Map<String, Object> result = new HashMap<>();
        result.put("matched", suggestion != null);
        result.put("matchedKeyword", suggestion != null ? suggestion.getMatchedKeyword() : null);
        result.put("matchedKeywords", suggestion != null ? suggestion.getMatchedKeywords() : null);
        result.put("categoryId", suggestion != null ? suggestion.getCategoryId() : null);
        result.put("categoryName", suggestion != null ? suggestion.getCategoryName() : null);
        result.put("categoryType", suggestion != null ? suggestion.getCategoryType() : null);

        if (suggestion != null) {
            Long categoryId = suggestion.getCategoryId();
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", categoryId);
            categoryMap.put("name", suggestion.getCategoryName());
            categoryMap.put("type", suggestion.getCategoryType());
            result.put("category", categoryMap);
        } else {
            result.put("category", null);
        }

        return Result.success(result);
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(excelService.importTransactions(user.getId(), accountBookId, file));
    }

    @GetMapping("/export")
    public void exportTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            HttpServletResponse response) throws IOException {
        excelService.exportTransactions(user.getId(), accountBookId, startDate, endDate, response);
    }

    @PostMapping("/export-tasks")
    public Result<TransactionExportTask> submitExportTask(
            @AuthenticationPrincipal User user,
            @RequestBody TransactionExportRequest request) {
        return Result.success("导出任务已提交，完成后将发送到您的邮箱", exportTaskService.submit(user, request));
    }

    @GetMapping("/export-tasks")
    public Result<List<TransactionExportTask>> listExportTasks(@AuthenticationPrincipal User user) {
        return Result.success(exportTaskService.listRecent(user));
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        excelService.downloadTemplate(response);
    }
}
