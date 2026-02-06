package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * Custom UserRepository interface for JDBC-based implementation
 * No JPA dependencies - pure JDBC operations
 */
public interface UserRepository {


    User saveUser(User user);
    User updateUser(User user);


    Optional<User> findById(Long id);


    boolean existsById(Long id);


    List<User> findAll();

    List<User> findAll(Sort sort);


    Page<User> findAll(Pageable pageable);


    long count();


    void deleteById(Long id);


    void delete(User user);


    Optional<User> findByEmail(String email);


    Optional<User> findByUsername(String username);


    boolean existsByEmail(String email);


    boolean existsByUsername(String username);


    Long countByIsActive(boolean isActive);

    Page<User> findByRole(String role, Pageable pageable);


    Page<User> searchUsers(String keyword, Pageable pageable);


    /**
     * Update only the password for a user by ID
     * @param userId the user's ID
     * @param newPassword the new hashed password
     * @return true if update was successful, false otherwise
     */
    boolean updatePassword(Long userId, String newPassword);

    int batchUpdate(List<User> users);
    Page<User> findByIsActive(boolean isActive, Pageable pageable);
}