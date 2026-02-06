package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.ChangePasswordRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.LoginResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserLoginRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UpdateUserRoleRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Address;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@RestController("moduleUserController")
@RequestMapping("v1/users")
@Tag(name = "User Management", description = "Operations related to user accounts")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        // Service returns Optional<UserDto>
        Optional<UserDto> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found with id: " + id));
        }
        UserDto dto = userOpt.get();
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", dto));
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction", example = "ASC")
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        // Create Pageable with explicit parameters
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<UserDto> users = userService.getAllUsers(pageable);
        PaginatedResponse<UserDto> paginatedResponse = PaginatedResponse.from(users);

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", paginatedResponse)
        );
    }



    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request,
                                                           UriComponentsBuilder uriBuilder) {
        // RegisterRequest is defined in modules.auth and used by the service
        UserDto created = userService.createUser(request);
        var uri = uriBuilder.path("/api/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("User created successfully", created));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        LoginResponse loggedIn = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", loggedIn));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Change user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user by ID")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        // Service accepts UserUpdateRequest and returns UserDto
        UserDto updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role by ID")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        UserDto updated = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Register user")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody UserCreateRequest request, UriComponentsBuilder uriBuilder) {
        UserDto created = userService.createUser(request);
        var uri = uriBuilder.path("/api/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("User registered successfully", created));
    }

    @GetMapping("/{id}/addresses")
    @Operation(summary = "Get all addresses for a user")
    public ResponseEntity<ApiResponse<List<Address>>> getUserAddresses(@PathVariable Long id) {
        Optional<UserDto> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found with id: " + id));
        }
        UserDto user = userOpt.get();
        List<Address> addresses = user.getAddresses();
        return ResponseEntity.ok(ApiResponse.success("User addresses fetched successfully", addresses));
    }
}