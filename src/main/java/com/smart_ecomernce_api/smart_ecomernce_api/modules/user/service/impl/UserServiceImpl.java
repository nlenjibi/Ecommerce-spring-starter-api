package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.impl;


import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.SecurityUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.ChangePasswordRequest;
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
@Slf4j
@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    private final UserRepository userRepository;


    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
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
        user.setRole(Role.USER);
        userRepository.save(user);

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
        userRepository.save(user);
        log.info("User updated with id: {}", userId);

        return userMapper.toDto(user);

    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
//
        if (!SecurityUtils.verifyPassword(request.getNewPassword(),request.getOldPassword())) {
            throw new ResourceNotFoundException("Password does not match");
        }
        var hashPassword= SecurityUtils.hashPasswordWithSalt(request.getNewPassword(), SecurityUtils.generateSalt());
        user.setPassword(hashPassword);
        userRepository.save(user);
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
}

