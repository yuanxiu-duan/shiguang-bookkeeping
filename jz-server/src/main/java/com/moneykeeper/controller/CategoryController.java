package com.moneykeeper.controller;

import com.moneykeeper.dto.Result;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.entity.User;
import com.moneykeeper.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<Map<String, List<Category>>> getCategories(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId) {
        return Result.success(categoryService.getCategories(user.getId(), accountBookId));
    }

    @GetMapping("/type/{type}")
    public Result<List<Category>> getCategoriesByType(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable CategoryType type) {
        return Result.success(categoryService.getCategoriesByType(user.getId(), type, accountBookId));
    }

    @PostMapping
    public Result<Category> createCategory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @RequestBody Category category) {
        return Result.success(categoryService.createCategory(user.getId(), accountBookId, category));
    }

    @PutMapping("/{id}")
    public Result<Category> updateCategory(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Category category) {
        return Result.success(categoryService.updateCategory(user.getId(), id, category));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountBookId,
            @PathVariable Long id) {
        categoryService.deleteCategory(user.getId(), accountBookId, id);
        return Result.success(null);
    }
}
