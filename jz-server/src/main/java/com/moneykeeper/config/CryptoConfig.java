package com.moneykeeper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneykeeper.filter.CryptoFilter;
import com.moneykeeper.util.RsaUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 加解密配置
 * - 创建 RsaUtil 单例（启动时生成 RSA 密钥对）
 * - 注册 CryptoFilter（order = -200，在 Spring Security 之前执行）
 */
@Configuration
public class CryptoConfig {

    @Bean
    public RsaUtil rsaUtil() {
        return new RsaUtil();
    }

    @Bean
    public FilterRegistrationBean<CryptoFilter> cryptoFilterRegistration(RsaUtil rsaUtil, ObjectMapper objectMapper) {
        CryptoFilter filter = new CryptoFilter(rsaUtil, objectMapper);
        FilterRegistrationBean<CryptoFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("cryptoFilter");
        return registration;
    }
}
