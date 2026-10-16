package com.moneykeeper.dto;

import com.moneykeeper.entity.Category.CategoryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionRequest {
    private Long accountBookId;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "类型不能为空")
    private CategoryType type;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String note;

    private String account;

    @NotNull(message = "交易日期不能为空")
    private LocalDate transactionDate;

    private List<Long> tagIds;
}
