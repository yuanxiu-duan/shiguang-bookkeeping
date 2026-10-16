package com.moneykeeper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.classification")
public class ClassificationRulesConfig {
    
    private List<Rule> rules = new ArrayList<>();
    
    @Data
    public static class Rule {
        private String categoryName;
        private List<String> keywords = new ArrayList<>();
        private String type = "EXPENSE";
    }
}
