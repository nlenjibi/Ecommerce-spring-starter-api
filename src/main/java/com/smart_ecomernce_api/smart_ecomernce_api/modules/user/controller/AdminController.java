
package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.controller;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.AdminDashboardDto;
import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/hello")
    @Operation(summary = "Hello Admin", description = "Test endpoint for admin access")
    public ResponseEntity<Map<String, String>> sayHello() {
        log.debug("Admin hello endpoint called");
        return ResponseEntity.ok(Map.of("message", "Hello Admin!"));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard", description = "Get admin dashboard statistics")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        log.info("Fetching admin dashboard");
        AdminDashboardDto dashboard = adminService.getDashboardStats();
        return ResponseEntity.ok(dashboard);
    }
}