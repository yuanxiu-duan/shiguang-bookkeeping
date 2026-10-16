package com.moneykeeper.controller;

import com.moneykeeper.dto.BudgetRequest;
import com.moneykeeper.dto.Result;
import com.moneykeeper.entity.Budget;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/current")
    public Result<List<Budget>> getCurrentMonthBudgets(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId) {
        return Result.success(budgetService.getCurrentMonthBudgets(user.getId(), accountBookId));
    }

    @GetMapping("/month/{month}")
    public Result<List<Budget>> getBudgetsByMonth(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable String month) {
        return Result.success(budgetService.getBudgetsByMonth(user.getId(), month, accountBookId));
    }

    @PostMapping
    public Result<Budget> createBudget(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BudgetRequest request) {
        return Result.success(budgetService.createBudget(user.getId(), request));
    }

    @PutMapping("/{id}")
    public Result<Budget> updateBudget(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id,
            @RequestBody BudgetRequest request) {
        return Result.success(budgetService.updateBudget(user.getId(), accountBookId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteBudget(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id) {
        budgetService.deleteBudget(user.getId(), accountBookId, id);
        return Result.success(null);
    }
}
