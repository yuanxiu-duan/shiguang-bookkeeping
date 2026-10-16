package com.moneykeeper.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "budget_alerts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category_id", "month", "alert_date"})
})
public class BudgetAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_book_id")
    private Long accountBookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "alert_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate alertDate;

    @Column(name = "is_notified", nullable = false)
    private Boolean isNotified = false;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "budget_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "used_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal usedAmount;

    @Column(name = "percentage", nullable = false)
    private Integer percentage;

    @Column(name = "over_amount", precision = 10, scale = 2)
    private BigDecimal overAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Transient
    private Category category;

    public Boolean getIsNotified() {
        return isNotified;
    }

    public void setIsNotified(Boolean notified) {
        isNotified = notified;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }
}
