package com.moneykeeper.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionExportRequest {
    private Long accountBookId;
    private LocalDate startDate;
    private LocalDate endDate;
}
