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

    // ==================== Basic CRUD Operations ====================

    /**
     * Save a user (insert if new, update if existing)
     * @param user User entity to save
     * @return Saved user with generated ID if new
     */
    User save(User user);

    /**
     * Save multiple users
     * @param users Iterable of users to save
     * @return List of saved users
     */
    List<User> saveAll(Iterable<User> users);

    /**
     * Find user by ID
     * @param id User ID
     * @return Optional containing user if found
     */
    Optional<User> findById(Long id);

    /**
     * Check if user exists by ID
     * @param id User ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);

    /**
     * Find all users
     * @return List of all users
     */
    List<User> findAll();

    /**
     * Find all users with sorting
     * @param sort Sort specification
     * @return Sorted list of users
     */
    List<User> findAll(Sort sort);

    /**
     * Find all users with pagination
     * @param pageable Pagination and sorting specification
     * @return Page of users
     */
    Page<User> findAll(Pageable pageable);


    long count();

    /**
     * Delete user by ID
     * @param id User ID to delete
     */
    void deleteById(Long id);

    /**
     * Delete a user
     * @param user User entity to delete
     */
    void delete(User user);


    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email exists
     * @param email Email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists
     * @param username Username to check
     * @return true if exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Find active user by ID
     * @param id User ID
     * @return Optional containing active user if found
     */
    Optional<User> findActiveById(Long id);

    /**
     * Find users with filters and pagination
     * @param firstName First name filter (partial match, case-insensitive)
     * @param lastName Last name filter (partial match, case-insensitive)
     * @param role Role filter (exact match)
     * @param pageable Pagination and sorting specification
     * @return Page of filtered users
     */
    Page<User> findUsersWithFilters(String firstName, String lastName, String role, Pageable pageable);

    /**
     * Count users by active status
     * @param isActive Active status
     * @return Count of users with given status
     */
    Long countByIsActive(boolean isActive);

    /**
     * Find all active users
     * @return List of active users
     */
    List<User> findAllActiveUsers();

    /**
     * Find users by role
     * @param role User role
     * @return List of users with given role
     */
    List<User> findByRole(String role);

    /**
     * Find users by role with pagination
     * @param role User role
     * @param pageable Pagination specification
     * @return Page of users with given role
     */
    Page<User> findByRole(String role, Pageable pageable);

    /**
     * Search users by keyword (searches in firstName, lastName, email, username)
     * @param keyword Search keyword
     * @param pageable Pagination specification
     * @return Page of matching users
     */
    Page<User> searchUsers(String keyword, Pageable pageable);

    /**
     * Update user password
     * @param userId User ID
     * @param newPassword New password (should be hashed before calling)
     * @return true if updated successfully
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * Update user active status
     * @param userId User ID
     * @param isActive New active status
     * @return true if updated successfully
     */
    boolean updateActiveStatus(Long userId, boolean isActive);

    /**
     * Find users created between dates
     * @param startDate Start date time
     * @param endDate End date time
     * @return List of users created in the date range
     */
    List<User> findByCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Batch update users
     * @param users List of users to update
     * @return Number of users updated
     */
    int batchUpdate(List<User> users);
}