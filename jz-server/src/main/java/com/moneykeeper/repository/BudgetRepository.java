package com.moneykeeper.repository;

import com.moneykeeper.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonth(Long userId, String month);
    Optional<Budget> findByUserIdAndMonthAndCategoryId(Long userId, String month, Long categoryId);
    boolean existsByUserIdAndMonthAndCategoryId(Long userId, String month, Long categoryId);

    List<Budget> findByUserIdAndAccountBookIdAndMonth(Long userId, Long accountBookId, String month);
    boolean existsByUserIdAndAccountBookIdAndMonthAndCategoryId(Long userId, Long accountBookId, String month, Long categoryId);
}
