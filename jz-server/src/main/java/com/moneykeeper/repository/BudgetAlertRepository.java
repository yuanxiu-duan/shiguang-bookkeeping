package com.moneykeeper.repository;

import com.moneykeeper.entity.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {

    List<BudgetAlert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    List<BudgetAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BudgetAlert b " +
           "WHERE b.userId = :userId AND b.categoryId = :categoryId AND b.month = :month AND b.alertDate = :alertDate")
    boolean existsByUserIdAndCategoryIdAndMonthAndAlertDate(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("month") String month,
            @Param("alertDate") LocalDate alertDate);

    long countByUserIdAndIsReadFalse(Long userId);

    List<BudgetAlert> findByUserIdAndAccountBookIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Long accountBookId);

    List<BudgetAlert> findByUserIdAndAccountBookIdOrderByCreatedAtDesc(Long userId, Long accountBookId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BudgetAlert b " +
           "WHERE b.userId = :userId AND b.accountBookId = :accountBookId AND b.categoryId = :categoryId AND b.month = :month AND b.alertDate = :alertDate")
    boolean existsByUserIdAndAccountBookIdAndCategoryIdAndMonthAndAlertDate(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("categoryId") Long categoryId,
            @Param("month") String month,
            @Param("alertDate") LocalDate alertDate);

    long countByUserIdAndAccountBookIdAndIsReadFalse(Long userId, Long accountBookId);
}
