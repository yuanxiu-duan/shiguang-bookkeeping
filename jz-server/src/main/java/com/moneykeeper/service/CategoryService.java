package com.moneykeeper.service;

import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.repository.CategoryRepository;
import com.moneykeeper.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountBookService accountBookService;
    private final AccountBookMemberService memberService;

    public static final String DEFAULT_CATEGORY_NAME_EXPENSE = "其他支出";
    public static final String DEFAULT_CATEGORY_NAME_INCOME = "其他";

    private static final List<Object[]> DEFAULT_EXPENSE_CATEGORIES = List.of(
            new Object[]{"吃喝", "🍽️", 1},
            new Object[]{"交通", "🚗", 2},
            new Object[]{"孩子", "👶", 3},
            new Object[]{"服饰鞋包", "👕", 4},
            new Object[]{"化妆护肤", "💄", 5},
            new Object[]{"日用品", "🛒", 6},
            new Object[]{"发红包", "🧧", 7},
            new Object[]{"人情往来", "🤝", 8},
            new Object[]{"话费", "📱", 9},
            new Object[]{"娱乐", "🎮", 10},
            new Object[]{"学习", "📚", 11},
            new Object[]{"医疗", "💊", 12},
            new Object[]{"水电燃气房租物业", "🏠", 13},
            new Object[]{"快递", "📦", 14},
            new Object[]{"保险", "🛡️", 15},
            new Object[]{DEFAULT_CATEGORY_NAME_EXPENSE, "📤", 99}
    );

    private static final List<Object[]> DEFAULT_INCOME_CATEGORIES = List.of(
            new Object[]{"工资", "💰", 1},
            new Object[]{"投资", "📈", 2},
            new Object[]{"奖金", "🎁", 3},
            new Object[]{"兼职", "💼", 4},
            new Object[]{"收红包", "🧧", 5},
            new Object[]{"父母资助", "💕", 6},
            new Object[]{DEFAULT_CATEGORY_NAME_INCOME, "📥", 99}
    );

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
                           AccountBookService accountBookService, AccountBookMemberService memberService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.accountBookService = accountBookService;
        this.memberService = memberService;
    }

    public Map<String, List<Category>> getCategories(Long userId, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Long finalAccountBookId = accountBookId;
        List<Category> allCategories = categoryRepository.findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(accountBookId);

        List<Category> userCategories = allCategories.stream()
                .filter(c -> c.getUserId() == null || c.getUserId().equals(userId) || memberService.hasAccess(userId, finalAccountBookId))
                .collect(Collectors.toList());

        Map<String, List<Category>> result = new HashMap<>();
        result.put("EXPENSE", userCategories.stream()
                .filter(c -> c.getType() == CategoryType.EXPENSE)
                .collect(Collectors.toList()));
        result.put("INCOME", userCategories.stream()
                .filter(c -> c.getType() == CategoryType.INCOME)
                .collect(Collectors.toList()));

        return result;
    }

    public List<Category> getCategoriesByType(Long userId, CategoryType type, Long accountBookId) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        Long finalAccountBookId = accountBookId;
        List<Category> allCategories = categoryRepository.findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(accountBookId);
        return allCategories.stream()
                .filter(c -> (c.getUserId() == null || memberService.hasAccess(userId, finalAccountBookId)) && c.getType() == type)
                .collect(Collectors.toList());
    }

    public Category createCategory(Long userId, Long accountBookId, Category category) {
        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        category.setUserId(userId);
        category.setAccountBookId(accountBookId);
        category.setIsDefault(false);
        if (category.getSortOrder() == null) {
            category.setSortOrder(100);
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long userId, Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        if (Boolean.TRUE.equals(existing.getIsDefault())) {
            throw new IllegalArgumentException("默认分类不能修改");
        }

        if (existing.getAccountBookId() != null) {
            checkAccess(userId, existing.getAccountBookId());
        } else if (existing.getUserId() != null && !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改此分类");
        }

        existing.setName(category.getName());
        existing.setIcon(category.getIcon());
        if (category.getSortOrder() != null) {
            existing.setSortOrder(category.getSortOrder());
        }

        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(Long userId, Long accountBookId, Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        if (accountBookId == null) {
            accountBookId = accountBookService.getDefaultAccountBook(userId).getId();
        }

        checkAccess(userId, accountBookId);

        if (existing.getAccountBookId() != null && !existing.getAccountBookId().equals(accountBookId)) {
            throw new IllegalArgumentException("无权限删除此分类");
        }

        // 默认分类允许从当前账本删除，但需要先将其下交易迁移到该账本的其他默认分类
        // 注意：默认分类是按 (accountBookId, type, isDefault=true) 维度独立存在的
        // 删除当前账本的默认分类不会影响其他账本的默认分类
        Category fallbackCategory = findFallbackCategory(accountBookId, existing.getType(), id);

        transactionRepository.updateCategoryIdByAccountBookIdAndCategoryId(
                accountBookId, id, fallbackCategory.getId());

        categoryRepository.delete(existing);
    }

    /**
     * 查找兜底分类：优先取同账本、同类型、其他默认分类；找不到则取/创建"其他支出/其他"默认分类。
     */
    private Category findFallbackCategory(Long accountBookId, CategoryType type, Long excludeId) {
        List<Category> candidates = categoryRepository
                .findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(accountBookId).stream()
                .filter(c -> c.getType() == type)
                .filter(c -> !c.getId().equals(excludeId))
                .filter(c -> c.getAccountBookId() == null || c.getAccountBookId().equals(accountBookId))
                .sorted((a, b) -> {
                    // 当前账本分类优先于全局分类
                    int aLocal = a.getAccountBookId() != null && a.getAccountBookId().equals(accountBookId) ? 0 : 1;
                    int bLocal = b.getAccountBookId() != null && b.getAccountBookId().equals(accountBookId) ? 0 : 1;
                    if (aLocal != bLocal) return Integer.compare(aLocal, bLocal);
                    // 默认分类优先
                    int aDef = Boolean.TRUE.equals(a.getIsDefault()) ? 0 : 1;
                    int bDef = Boolean.TRUE.equals(b.getIsDefault()) ? 0 : 1;
                    if (aDef != bDef) return Integer.compare(aDef, bDef);
                    return Integer.compare(a.getSortOrder(), b.getSortOrder());
                })
                .collect(Collectors.toList());

        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }
        // 兜底：抛异常（理论上不会走到这里，因为每个账本初始化时都有"其他"分类）
        throw new IllegalStateException("无法找到兜底分类，请先初始化账本默认分类");
    }

    public Category getOrCreateDefaultCategory(Long userId, CategoryType type) {
        String defaultName = type == CategoryType.EXPENSE
                ? DEFAULT_CATEGORY_NAME_EXPENSE
                : DEFAULT_CATEGORY_NAME_INCOME;

        return categoryRepository.findByUserIdOrUserIdIsNullAndTypeAndName(userId, type, defaultName)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(defaultName);
                    newCategory.setType(type);
                    newCategory.setIsDefault(true);
                    newCategory.setSortOrder(999);
                    newCategory.setIcon(type == CategoryType.EXPENSE ? "📤" : "📥");
                    return categoryRepository.save(newCategory);
                });
    }

    public Category getOrCreateDefaultCategory(Long userId, Long accountBookId, CategoryType type) {
        if (accountBookId == null) {
            return getOrCreateDefaultCategory(userId, type);
        }

        String defaultName = type == CategoryType.EXPENSE
                ? DEFAULT_CATEGORY_NAME_EXPENSE
                : DEFAULT_CATEGORY_NAME_INCOME;

        return categoryRepository.findByAccountBookIdOrUserIdIsNullAndTypeAndName(accountBookId, type, defaultName)
                .stream().findFirst()
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(defaultName);
                    newCategory.setType(type);
                    newCategory.setIsDefault(true);
                    newCategory.setSortOrder(999);
                    newCategory.setIcon(type == CategoryType.EXPENSE ? "📤" : "📥");
                    newCategory.setAccountBookId(accountBookId);
                    newCategory.setUserId(userId);
                    return categoryRepository.save(newCategory);
                });
    }

    private void checkAccess(Long userId, Long accountBookId) {
        if (!memberService.hasAccess(userId, accountBookId)) {
            throw new IllegalArgumentException("您没有访问此账本的权限");
        }
    }

    @Transactional
    public void initDefaultCategories(Long userId, Long accountBookId) {
        for (Object[] item : DEFAULT_EXPENSE_CATEGORIES) {
            Category category = new Category();
            category.setUserId(userId);
            category.setAccountBookId(accountBookId);
            category.setName((String) item[0]);
            category.setType(CategoryType.EXPENSE);
            category.setIcon((String) item[1]);
            category.setSortOrder((Integer) item[2]);
            category.setIsDefault(true);
            categoryRepository.save(category);
        }

        for (Object[] item : DEFAULT_INCOME_CATEGORIES) {
            Category category = new Category();
            category.setUserId(userId);
            category.setAccountBookId(accountBookId);
            category.setName((String) item[0]);
            category.setType(CategoryType.INCOME);
            category.setIcon((String) item[1]);
            category.setSortOrder((Integer) item[2]);
            category.setIsDefault(true);
            categoryRepository.save(category);
        }
    }
}
