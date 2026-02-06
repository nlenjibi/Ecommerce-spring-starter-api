package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.impl;


import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.SecurityUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.ChangePasswordRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.LoginResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserLoginRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UserUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.mapper.UserMapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
@Slf4j
@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    private final UserRepository userRepository;


    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }


        var user = userMapper.toEntity(request);
        var hashPassword= SecurityUtils.hashPassword(user.getPassword());
        user.setPassword(hashPassword);
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                Role newRole = Role.valueOf(request.getRole().toUpperCase());
                user.setRole(newRole);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        } else {
            user.setRole(Role.USER);
        }
        userRepository.saveUser(user);

        log.info("User created with id: {}", user.getId());

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {

        var user= userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return Optional.of(userMapper.toDto(user));
    }
    @Transactional(readOnly = true)
    @Override
    @Cacheable(value = "users", key = "#username")
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", "username: " + username));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", "email: " + email));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    @Override
    public UserDto updateUser(Long userId, UserUpdateRequest request) {

        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userMapper.updateEntity(user, request);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                Role newRole = Role.valueOf(request.getRole().toUpperCase());
                user.setRole(newRole);
            } catch (IllegalArgumentException e) {
                // Handle invalid role string
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }

        userRepository.updateUser(user);
        log.info("User updated with id: {}", userId);

        return userMapper.toDto(user);

    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!SecurityUtils.verifyPassword(request.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Password does not match");
        }
        var hashPassword = SecurityUtils.hashPassword(request.getNewPassword());
        boolean updated = userRepository.updatePassword(userId, hashPassword);
        if (!updated) {
            throw new RuntimeException("Failed to update password for user with id: " + userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(UserLoginRequest request) {
        User user;
        if (request.getUsernameOrEmail() != null && request.getUsernameOrEmail().contains("@")) {
            user = userRepository.findByEmail(request.getUsernameOrEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));
        } else {
            user = userRepository.findByUsername(request.getUsernameOrEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));
        }

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new ResourceNotFoundException("User account is inactive");
        }

        if (!SecurityUtils.verifyPassword(request.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid credentials");
        }

        String sessionToken = generateSessionToken(user);
        UserDto userDto = userMapper.toDto(user);
        return LoginResponse.builder()
                .id(userDto.getId())
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .phoneNumber(userDto.getPhoneNumber())
                .address(userDto.getAddress())
                .role(userDto.getRole())
                .isActive(userDto.getIsActive())
                .createdAt(userDto.getCreatedAt())
                .updatedAt(userDto.getUpdatedAt())
                .sessionToken(sessionToken)
                .build();
    }

    private String generateSessionToken(User user) {
        // Very simple session token: UUID + userId + timestamp hash
        String raw = UUID.randomUUID() + "-" + user.getId() + "-" + System.currentTimeMillis();
        return SecurityUtils.hashPassword(raw);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
       return Optional.ofNullable(userRepository.findByUsername(username)
               .orElseThrow(() -> ResourceNotFoundException.forResource("User", "username: " + username)));
        }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {

        return Optional.ofNullable(userRepository.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", "email: " + email)));

    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {

        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String username) {
        return userRepository.existsByEmail(username);
    }

    @Override
    public UserDto updateUserRole(Long userId, com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                Role newRole = Role.valueOf(request.getRole().toUpperCase());
                user.setRole(newRole);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }

        userRepository.updateUser(user);
        log.info("User role updated for user with id: {}", userId);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> filterUsersByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> filterUsersByIsActive(boolean isActive, Pageable pageable) {
        return userRepository.findByIsActive(isActive, pageable)
                .map(userMapper::toDto);
    }
}
