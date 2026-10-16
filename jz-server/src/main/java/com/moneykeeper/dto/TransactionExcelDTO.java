package com.moneykeeper.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ColumnWidth(20)
public class TransactionExcelDTO {

    @ExcelProperty(index = 0, value = "交易日期")
    private String transactionDate;

    @ExcelProperty(index = 1, value = "类型")
    private String type;

    @ExcelProperty(index = 2, value = "金额")
    private BigDecimal amount;

    @ExcelProperty(index = 3, value = "分类名称")
    private String categoryName;

    @ExcelProperty(index = 4, value = "备注")
    private String note;

    @ExcelProperty(index = 5, value = "创建人")
    private String createdBy;

    @ExcelProperty(index = 6, value = "账户")
    private String account;
}
