package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.Data;

import java.time.Duration;

/**
 * Cart Configuration
 * Centralized configuration for cart-related settings
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableAsync
public class CartConfig {

    /**
     * Cart properties from application.properties
     */
    @Bean
    @ConfigurationProperties(prefix = "cart")
    public CartProperties cartProperties() {
        return new CartProperties();
    }



    /**
     * Cart Properties Class
     */
    @Data
    public static class CartProperties {
        private int maxItems = 100;
        private int abandonedThresholdHours = 24;
        private int expirationDays = 90;
        private int shareLinkExpiryHours = 72;
        private int validationCacheTtlMinutes = 5;
        private boolean priceRefreshOnValidation = true;
        private boolean autoCleanupEnabled = true;
        private boolean mergeOnLogin = true;
    }
}

/**
 * CORS Configuration
 */
@Configuration
class CorsConfig {

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer corsConfigurer() {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}

/**
 * Async Configuration for background tasks
 */
@Configuration
class AsyncConfig implements org.springframework.scheduling.annotation.AsyncConfigurer {

    @Bean(name = "taskExecutor")
    public java.util.concurrent.Executor taskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cart-async-");
        executor.initialize();
        return executor;
    }

    @Override
    public java.util.concurrent.Executor getAsyncExecutor() {
        return taskExecutor();
    }
}

/**
 * Jackson Configuration for JSON serialization
 */


/**
 * Validation Configuration
 */
@Configuration
class ValidationConfig {

    @Bean
    public org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator() {
        return new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
    }
}