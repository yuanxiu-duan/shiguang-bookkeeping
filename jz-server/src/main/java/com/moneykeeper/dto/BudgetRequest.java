package com.moneykeeper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {
    private Long accountBookId;

    private Long categoryId;

    @NotNull(message = "月份不能为空，格式：yyyy-MM")
    private String month;

    @NotNull(message = "预算额度不能为空")
    @Positive(message = "预算额度必须大于0")
    private BigDecimal amountLimit;

    private Integer alertThreshold = 80;
}
