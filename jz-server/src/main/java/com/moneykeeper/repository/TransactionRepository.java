package com.moneykeeper.repository;

import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndCategoryIdAndTransactionDateBetween(
            Long userId, Long categoryId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByUserIdAndTypeAndTransactionDateBetween(
            @Param("userId") Long userId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.categoryId")
    List<Object[]> sumByUserIdAndTypeAndTransactionDateBetweenGroupByCategory(
            @Param("userId") Long userId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.transactionDate, t.type, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.transactionDate, t.type ORDER BY t.transactionDate")
    List<Object[]> sumByUserIdAndTransactionDateBetweenGroupByDateAndType(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId, Pageable pageable);

    List<Transaction> findByUserIdAndIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long userId, List<Long> ids, LocalDate startDate, LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, t.type, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.userId = :userId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m'), t.type " +
           "ORDER BY month")
    List<Object[]> getMonthlyTrend(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.userId = :userId AND t.categoryId = :categoryId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') " +
           "ORDER BY month")
    List<Object[]> getCategoryMonthlyTrend(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = :newCategoryId WHERE t.userId = :userId AND t.categoryId = :oldCategoryId")
    int updateCategoryIdByUserIdAndCategoryId(
            @Param("userId") Long userId,
            @Param("oldCategoryId") Long oldCategoryId,
            @Param("newCategoryId") Long newCategoryId);

    Page<Transaction> findByUserIdAndAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByUserIdAndAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, Long accountBookId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByUserIdAndAccountBookIdAndTypeAndTransactionDateBetween(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.categoryId")
    List<Object[]> sumByUserIdAndAccountBookIdAndTypeAndTransactionDateBetweenGroupByCategory(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.transactionDate, t.type, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.transactionDate, t.type ORDER BY t.transactionDate")
    List<Object[]> sumByUserIdAndAccountBookIdAndTransactionDateBetweenGroupByDateAndType(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, t.type, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m'), t.type " +
           "ORDER BY month")
    List<Object[]> getMonthlyTrendByAccountBookId(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("startDate") LocalDate startDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.categoryId = :categoryId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') " +
           "ORDER BY month")
    List<Object[]> getCategoryMonthlyTrendByAccountBookId(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate);

    List<Transaction> findByUserIdAndAccountBookIdAndIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long userId, Long accountBookId, List<Long> ids, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = :newCategoryId WHERE t.userId = :userId AND t.accountBookId = :accountBookId AND t.categoryId = :oldCategoryId")
    int updateCategoryIdByUserIdAndAccountBookIdAndCategoryId(
            @Param("userId") Long userId,
            @Param("accountBookId") Long accountBookId,
            @Param("oldCategoryId") Long oldCategoryId,
            @Param("newCategoryId") Long newCategoryId);

    Page<Transaction> findByUserIdAndAccountBookIdOrderByTransactionDateDescCreatedAtDesc(
            Long userId, Long accountBookId, Pageable pageable);

    Page<Transaction> findByUserIdAndAccountBookIdAndTypeOrderByTransactionDateDescCreatedAtDesc(
            Long userId, Long accountBookId, CategoryType type, Pageable pageable);

    Page<Transaction> findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long accountBookId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByAccountBookIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long accountBookId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.accountBookId = :accountBookId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByAccountBookIdAndTypeAndTransactionDateBetween(
            @Param("accountBookId") Long accountBookId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.accountBookId = :accountBookId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.categoryId")
    List<Object[]> sumByAccountBookIdAndTypeAndTransactionDateBetweenGroupByCategory(
            @Param("accountBookId") Long accountBookId,
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.transactionDate, t.type, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.accountBookId = :accountBookId AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.transactionDate, t.type ORDER BY t.transactionDate")
    List<Object[]> sumByAccountBookIdAndTransactionDateBetweenGroupByDateAndType(
            @Param("accountBookId") Long accountBookId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, t.type, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.accountBookId = :accountBookId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m'), t.type " +
           "ORDER BY month")
    List<Object[]> getMonthlyTrendByBookId(
            @Param("accountBookId") Long accountBookId,
            @Param("startDate") LocalDate startDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') as month, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t WHERE t.accountBookId = :accountBookId AND t.categoryId = :categoryId AND t.transactionDate >= :startDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m') " +
           "ORDER BY month")
    List<Object[]> getCategoryMonthlyTrendByBookId(
            @Param("accountBookId") Long accountBookId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate);

    List<Transaction> findByAccountBookIdAndIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long accountBookId, List<Long> ids, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("UPDATE Transaction t SET t.categoryId = :newCategoryId WHERE t.accountBookId = :accountBookId AND t.categoryId = :oldCategoryId")
    int updateCategoryIdByAccountBookIdAndCategoryId(
            @Param("accountBookId") Long accountBookId,
            @Param("oldCategoryId") Long oldCategoryId,
            @Param("newCategoryId") Long newCategoryId);

    Page<Transaction> findByAccountBookIdOrderByTransactionDateDescCreatedAtDesc(
            Long accountBookId, Pageable pageable);

    Page<Transaction> findByAccountBookIdAndTypeOrderByTransactionDateDescCreatedAtDesc(
            Long accountBookId, CategoryType type, Pageable pageable);

    Page<Transaction> findByAccountBookIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long accountBookId, CategoryType type, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByAccountBookIdAndTransactionDateBetween(
            Long accountBookId, LocalDate startDate, LocalDate endDate);
}
