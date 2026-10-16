package com.moneykeeper.repository;

import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrUserIdIsNullOrderBySortOrderAsc(Long userId);
    List<Category> findByUserIdOrUserIdIsNullAndTypeOrderBySortOrderAsc(Long userId, CategoryType type);
    List<Category> findByTypeOrderBySortOrderAsc(CategoryType type);

    Optional<Category> findByTypeAndName(CategoryType type, String name);

    Optional<Category> findByUserIdIsNullAndTypeAndName(CategoryType type, String name);

    Optional<Category> findByUserIdOrUserIdIsNullAndTypeAndName(Long userId, CategoryType type, String name);

    List<Category> findByTypeAndNameIn(CategoryType type, List<String> names);

    List<Category> findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(Long accountBookId);
    List<Category> findByAccountBookIdOrUserIdIsNullAndTypeAndName(Long accountBookId, CategoryType type, String name);
}
