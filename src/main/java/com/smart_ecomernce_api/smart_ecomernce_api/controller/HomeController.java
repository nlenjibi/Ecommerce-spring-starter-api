package com.smart_ecomernce_api.smart_ecomernce_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping({"", "/"})
    public String home() {
        return "index";
    }

    @GetMapping("/api/info")
    @ResponseBody
    public Map<String, Object> apiInfo() {
        return Map.of(
                "app", "Smart E-Commerce Platform API",
                "version", "1.0.0",
                "status", "running",
                "timestamp", LocalDateTime.now(),
                "apiBase", "/api/v1",
                "docs", Map.of(
                        "swagger", "/swagger-ui.html",
                        "openapi", "/v3/api-docs"
                ),
                "importantEndpoints", List.of(
                        "/api/v1/product",
                        "/api/v1/categories",
                        "/orders/my-orders",
                        "/carts/{cartId}"
                )
        );
    }
}