package com.smart_ecomernce_api.smart_ecomernce_api.config;//package com.smart_ecomernce_api.Smart_ecommerce_api.config;
//
//import com.stripe.Stripe;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Stripe Configuration
// */
//@Configuration
//@Slf4j
//public class StripeConfig {
//
//    @Value("${stripe.secret-key:}")
//    private String secretKey;
//
//    @PostConstruct
//    public void init() {
//        if (secretKey != null && !secretKey.isEmpty()) {
//            Stripe.apiKey = secretKey;
//            log.info("Stripe API initialized");
//        } else {
//            log.warn("Stripe secret key not configured");
//        }
//    }
//}
