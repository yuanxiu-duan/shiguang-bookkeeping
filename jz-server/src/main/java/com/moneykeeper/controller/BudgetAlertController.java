package com.moneykeeper.controller;

import com.moneykeeper.dto.Result;
import com.moneykeeper.entity.BudgetAlert;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.BudgetAlertService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets/alerts")
public class BudgetAlertController {

    private final BudgetAlertService budgetAlertService;

    public BudgetAlertController(BudgetAlertService budgetAlertService) {
        this.budgetAlertService = budgetAlertService;
    }

    @GetMapping
    public Result<List<BudgetAlert>> getAlerts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        if (all) {
            return Result.success(budgetAlertService.getAllAlerts(user.getId(), accountBookId));
        }
        return Result.success(budgetAlertService.getUnreadAlerts(user.getId(), accountBookId));
    }

    @GetMapping("/count")
    public Result<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId) {
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", budgetAlertService.getUnreadCount(user.getId(), accountBookId));
        return Result.success(result);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id) {
        budgetAlertService.markAsRead(user.getId(), accountBookId, id);
        return Result.success(null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId) {
        budgetAlertService.markAllAsRead(user.getId(), accountBookId);
        return Result.success(null);
    }
}
