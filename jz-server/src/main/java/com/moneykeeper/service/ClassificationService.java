package com.moneykeeper.service;

import com.moneykeeper.config.ClassificationRulesConfig;
import com.moneykeeper.entity.Category;
import com.moneykeeper.entity.Category.CategoryType;
import com.moneykeeper.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClassificationService {

    private final ClassificationRulesConfig rulesConfig;
    private final CategoryRepository categoryRepository;
    private final AccountBookService accountBookService;

    public ClassificationService(ClassificationRulesConfig rulesConfig, CategoryRepository categoryRepository,
                                 AccountBookService accountBookService) {
        this.rulesConfig = rulesConfig;
        this.categoryRepository = categoryRepository;
        this.accountBookService = accountBookService;
    }

    public ClassificationSuggestion suggestCategory(Long userId, Long accountBookId, String note) {
        if (note == null || note.trim().isEmpty()) {
            return null;
        }

        String noteLower = note.toLowerCase();

        List<ClassificationRulesConfig.Rule> rules = rulesConfig.getRules();
        if (rules.isEmpty()) {
            rules = getDefaultRules();
        }

        Map<String, Category> defaultCategoryMap = getDefaultCategoryMap(userId, accountBookId);

        for (ClassificationRulesConfig.Rule rule : rules) {
            for (String keyword : rule.getKeywords()) {
                if (noteLower.contains(keyword.toLowerCase())) {
                    Category category = defaultCategoryMap.get(rule.getCategoryName());
                    if (category != null) {
                        return new ClassificationSuggestion(
                            category.getId(),
                            category.getName(),
                            category.getType(),
                            rule.getKeywords(),
                            keyword
                        );
                    }
                }
            }
        }

        return null;
    }

    private List<ClassificationRulesConfig.Rule> getDefaultRules() {
        List<ClassificationRulesConfig.Rule> rules = new ArrayList<>();

        ClassificationRulesConfig.Rule foodRule = new ClassificationRulesConfig.Rule();
        foodRule.setCategoryName("餐饮");
        foodRule.setType("EXPENSE");
        foodRule.setKeywords(Arrays.asList("星巴克", "瑞幸", "奶茶", "吃饭", "餐厅", "食堂", "外卖", "kfc", "麦当劳", "必胜客"));
        rules.add(foodRule);

        ClassificationRulesConfig.Rule takeoutRule = new ClassificationRulesConfig.Rule();
        takeoutRule.setCategoryName("外卖");
        takeoutRule.setType("EXPENSE");
        takeoutRule.setKeywords(Arrays.asList("美团", "饿了么", "美团外卖", "饿了么外卖"));
        rules.add(takeoutRule);

        ClassificationRulesConfig.Rule transportRule = new ClassificationRulesConfig.Rule();
        transportRule.setCategoryName("交通");
        transportRule.setType("EXPENSE");
        transportRule.setKeywords(Arrays.asList("滴滴", "打车", "地铁", "公交", "加油", "停车", "高铁", "机票"));
        rules.add(transportRule);

        ClassificationRulesConfig.Rule shoppingRule = new ClassificationRulesConfig.Rule();
        shoppingRule.setCategoryName("日用品");
        shoppingRule.setType("EXPENSE");
        shoppingRule.setKeywords(Arrays.asList("淘宝", "天猫", "京东", "拼多多", "超市", "便利店"));
        rules.add(shoppingRule);

        ClassificationRulesConfig.Rule salaryRule = new ClassificationRulesConfig.Rule();
        salaryRule.setCategoryName("工资");
        salaryRule.setType("INCOME");
        salaryRule.setKeywords(Arrays.asList("工资", "薪资", "奖金", "年终奖", "绩效"));
        rules.add(salaryRule);

        ClassificationRulesConfig.Rule entertainmentRule = new ClassificationRulesConfig.Rule();
        entertainmentRule.setCategoryName("娱乐");
        entertainmentRule.setType("EXPENSE");
        entertainmentRule.setKeywords(Arrays.asList("电影", "ktv", "游戏", "网吧", "旅游", "酒店"));
        rules.add(entertainmentRule);

        return rules;
    }

    private Map<String, Category> getDefaultCategoryMap(Long userId, Long accountBookId) {
        List<Category> categories;
        if (accountBookId != null) {
            categories = categoryRepository.findByAccountBookIdOrUserIdIsNullOrderBySortOrderAsc(accountBookId);
        } else {
            categories = categoryRepository.findByUserIdOrUserIdIsNullOrderBySortOrderAsc(userId);
        }
        Map<String, Category> map = new HashMap<>();

        for (Category cat : categories) {
            map.put(cat.getName(), cat);
        }

        return map;
    }

    public static class ClassificationSuggestion {
        private Long categoryId;
        private String categoryName;
        private CategoryType categoryType;
        private List<String> matchedKeywords;
        private String matchedKeyword;

        public ClassificationSuggestion(Long categoryId, String categoryName, CategoryType categoryType,
                                         List<String> matchedKeywords, String matchedKeyword) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.categoryType = categoryType;
            this.matchedKeywords = matchedKeywords;
            this.matchedKeyword = matchedKeyword;
        }

        public Long getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public CategoryType getCategoryType() { return categoryType; }
        public List<String> getMatchedKeywords() { return matchedKeywords; }
        public String getMatchedKeyword() { return matchedKeyword; }
    }
}
