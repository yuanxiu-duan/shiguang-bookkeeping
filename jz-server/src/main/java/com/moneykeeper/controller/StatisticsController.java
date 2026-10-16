package com.moneykeeper.controller;

import com.moneykeeper.dto.CategoryTrendData;
import com.moneykeeper.dto.CurvePoint;
import com.moneykeeper.dto.MonthlyStatistics;
import com.moneykeeper.dto.Result;
import com.moneykeeper.dto.TrendData;
import com.moneykeeper.dto.YearlyStatistics;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.StatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/monthly")
    public Result<MonthlyStatistics> getMonthlyStatistics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(required = false) String month) {
        return Result.success(statisticsService.getMonthlyStatistics(user.getId(), accountBookId, month));
    }

    @GetMapping("/yearly")
    public Result<YearlyStatistics> getYearlyStatistics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(required = false) Integer year) {
        return Result.success(statisticsService.getYearlyStatistics(user.getId(), accountBookId, year));
    }

    @GetMapping("/all")
    public Result<YearlyStatistics> getAllTimeStatistics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId) {
        return Result.success(statisticsService.getAllTimeStatistics(user.getId(), accountBookId));
    }

    @GetMapping("/curve")
    public Result<List<CurvePoint>> getCurveData(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "month") String mode,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {
        return Result.success(statisticsService.getCurveData(user.getId(), accountBookId, mode, month, year));
    }

    @GetMapping("/trend")
    public Result<List<TrendData>> getMonthlyTrend(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestParam(defaultValue = "12") int months) {
        return Result.success(statisticsService.getMonthlyTrend(user.getId(), accountBookId, months));
    }

    @GetMapping("/category-trend/{categoryId}")
    public Result<List<CategoryTrendData>> getCategoryTrend(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(statisticsService.getCategoryTrend(user.getId(), accountBookId, categoryId, months));
    }
}
