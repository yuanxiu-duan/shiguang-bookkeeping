package com.moneykeeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlyStatistics {
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;
    private BigDecimal balance;
    private List<Map<String, Object>> monthlyTrend;
    private List<Map<String, Object>> expenseByCategory;
    private List<Map<String, Object>> incomeByCategory;
}
