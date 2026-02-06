package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC-based implementation of UserRepository
 * Uses JdbcUtils for all database operations
 */
@Repository
public class UserRepositoryImpl implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final JdbcUtils jdbcUtils;

    // Table and column names
    private static final String TABLE_NAME = "users";
    private static final String COL_ID = "id";
    private static final String COL_EMAIL = "email";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_FIRST_NAME = "first_name";
    private static final String COL_LAST_NAME = "last_name";
    private static final String COL_PHONE_NUMBER = "phone_number";
    private static final String COL_ROLE = "role";
    private static final String COL_IS_ACTIVE = "is_active";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_UPDATED_AT = "updated_at";

    // Base SELECT query
    private static final String BASE_SELECT =
            "SELECT id, email, username, password, first_name, last_name, phone_number, " +
                    "role, is_active, created_at, updated_at FROM " + TABLE_NAME;

    public UserRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper to convert ResultSet to User entity
     */
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong(COL_ID));
        user.setEmail(rs.getString(COL_EMAIL));
        user.setUsername(rs.getString(COL_USERNAME));
        user.setPassword(rs.getString(COL_PASSWORD));
        user.setFirstName(rs.getString(COL_FIRST_NAME));
        user.setLastName(rs.getString(COL_LAST_NAME));
        user.setPhoneNumber(rs.getString(COL_PHONE_NUMBER));

        String roleStr = rs.getString(COL_ROLE);
        if (roleStr != null) {
            user.setRole(Role.valueOf(roleStr));
        }

        user.setIsActive(rs.getBoolean(COL_IS_ACTIVE));

        Timestamp createdAt = rs.getTimestamp(COL_CREATED_AT);
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp(COL_UPDATED_AT);
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return user;
    };

    // ==================== Basic CRUD Operations ====================

    @Override
    @Transactional
    public User saveUser(User user) {
            return insert(user);

    }
    @Override
    @Transactional
    public User updateUser(User user) {

            return update(user);

    }


    @Override
    public Optional<User> findById(Long id) {
        String query = BASE_SELECT + " WHERE id = ?";
        List<User> users = jdbcUtils.query(query, userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public boolean existsById(Long id) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE id = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<User> findAll() {
        return jdbcUtils.query(BASE_SELECT, userRowMapper);
    }

    @Override
    public List<User> findAll(Sort sort) {
        String query = BASE_SELECT + buildOrderByClause(sort);
        return jdbcUtils.query(query, userRowMapper);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        long total = count();

        String query = BASE_SELECT +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<User> users = jdbcUtils.query(query, userRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(users, pageable, total);
    }

    public List<User> findAllById(Iterable<Long> ids) {
        List<Long> idList = new ArrayList<>();
        ids.forEach(idList::add);

        if (idList.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = idList.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String query = BASE_SELECT + " WHERE id IN (" + placeholders + ")";
        return jdbcUtils.query(query, userRowMapper, idList.toArray());
    }

    @Override
    public long count() {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Long count = jdbcUtils.queryForObject(query, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String deleteAddressesQuery = "DELETE FROM addresses WHERE user_id = ?";
        QueryResult deleteAddressesResult = jdbcUtils.executePreparedQuery(deleteAddressesQuery, id);
        if (deleteAddressesResult.hasError()) {
            logger.error("Error deleting addresses for user id {}: {}", id, deleteAddressesResult.getError());
            throw new InvalidDataException("Failed to delete addresses for user with id " + id + ": " + deleteAddressesResult.getError());
        }

        String deleteProfileQuery = "DELETE FROM profiles WHERE id = ?";
        QueryResult deleteProfileResult = jdbcUtils.executePreparedQuery(deleteProfileQuery, id);
        if (deleteProfileResult.hasError()) {
            logger.error("Error deleting profile for user id {}: {}", id, deleteProfileResult.getError());
            throw new InvalidDataException("Failed to delete profile for user with id " + id + ": " + deleteProfileResult.getError());
        }

        String deleteCartItemsQuery = "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)";
        QueryResult deleteCartItemsResult = jdbcUtils.executePreparedQuery(deleteCartItemsQuery, id);
        if (deleteCartItemsResult.hasError()) {
            logger.error("Error deleting cart items for user id {}: {}", id, deleteCartItemsResult.getError());
            throw new InvalidDataException("Failed to delete cart items for user with id " + id + ": " + deleteCartItemsResult.getError());
        }

        String deleteCartsQuery = "DELETE FROM carts WHERE user_id = ?";
        QueryResult deleteCartsResult = jdbcUtils.executePreparedQuery(deleteCartsQuery, id);
        if (deleteCartsResult.hasError()) {
            logger.error("Error deleting carts for user id {}: {}", id, deleteCartsResult.getError());
            throw new InvalidDataException("Failed to delete carts for user with id " + id + ": " + deleteCartsResult.getError());
        }

        String deleteWishlistItemsQuery = "DELETE FROM wishlist_items WHERE user_id = ?";
        QueryResult deleteWishlistItemsResult = jdbcUtils.executePreparedQuery(deleteWishlistItemsQuery, id);
        if (deleteWishlistItemsResult.hasError()) {
            logger.error("Error deleting wishlist items for user id {}: {}", id, deleteWishlistItemsResult.getError());
            throw new InvalidDataException("Failed to delete wishlist items for user with id " + id + ": " + deleteWishlistItemsResult.getError());
        }

        String deleteReviewImagesQuery = "DELETE FROM review_images WHERE review_id IN (SELECT id FROM reviews WHERE user_id = ?)";
        QueryResult deleteReviewImagesResult = jdbcUtils.executePreparedQuery(deleteReviewImagesQuery, id);
        if (deleteReviewImagesResult.hasError()) {
            logger.error("Error deleting review images for user id {}: {}", id, deleteReviewImagesResult.getError());
            throw new InvalidDataException("Failed to delete review images for user with id " + id + ": " + deleteReviewImagesResult.getError());
        }

        String deleteReviewProsQuery = "DELETE FROM review_pros WHERE review_id IN (SELECT id FROM reviews WHERE user_id = ?)";
        QueryResult deleteReviewProsResult = jdbcUtils.executePreparedQuery(deleteReviewProsQuery, id);
        if (deleteReviewProsResult.hasError()) {
            logger.error("Error deleting review pros for user id {}: {}", id, deleteReviewProsResult.getError());
            throw new InvalidDataException("Failed to delete review pros for user with id " + id + ": " + deleteReviewProsResult.getError());
        }

        String deleteReviewConsQuery = "DELETE FROM review_cons WHERE review_id IN (SELECT id FROM reviews WHERE user_id = ?)";
        QueryResult deleteReviewConsResult = jdbcUtils.executePreparedQuery(deleteReviewConsQuery, id);
        if (deleteReviewConsResult.hasError()) {
            logger.error("Error deleting review cons for user id {}: {}", id, deleteReviewConsResult.getError());
            throw new InvalidDataException("Failed to delete review cons for user with id " + id + ": " + deleteReviewConsResult.getError());
        }

        String deleteReviewsQuery = "DELETE FROM reviews WHERE user_id = ?";
        QueryResult deleteReviewsResult = jdbcUtils.executePreparedQuery(deleteReviewsQuery, id);
        if (deleteReviewsResult.hasError()) {
            logger.error("Error deleting reviews for user id {}: {}", id, deleteReviewsResult.getError());
            throw new InvalidDataException("Failed to delete reviews for user with id " + id + ": " + deleteReviewsResult.getError());
        }

        String deleteOrderItemsQuery = "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id = ?)";
        QueryResult deleteOrderItemsResult = jdbcUtils.executePreparedQuery(deleteOrderItemsQuery, id);
        if (deleteOrderItemsResult.hasError()) {
            logger.error("Error deleting order items for user id {}: {}", id, deleteOrderItemsResult.getError());
            throw new InvalidDataException("Failed to delete order items for user with id " + id + ": " + deleteOrderItemsResult.getError());
        }

        String deleteOrdersQuery = "DELETE FROM orders WHERE user_id = ?";
        QueryResult deleteOrdersResult = jdbcUtils.executePreparedQuery(deleteOrdersQuery, id);
        if (deleteOrdersResult.hasError()) {
            logger.error("Error deleting orders for user id {}: {}", id, deleteOrdersResult.getError());
            throw new InvalidDataException("Failed to delete orders for user with id " + id + ": " + deleteOrdersResult.getError());
        }

        String query = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, id);
        if (result.hasError()) {
            logger.error("Error deleting user with id {}: {}", id, result.getError());
            throw new InvalidDataException("Failed to delete user with id " + id + ": " + result.getError());
        }

        if (result.getAffectedRows() <= 0) {
            logger.warn("Delete user with id {} did not affect any rows", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        logger.info("Deleted user with id: {}", id);
    }

    @Override
    @Transactional
    public void delete(User user) {
        deleteById(user.getId());
    }

    @Transactional
    public void deleteAllById(Iterable<Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Transactional
    public void deleteAll(Iterable<User> users) {
        users.forEach(this::delete);
    }

    @Transactional
    public void deleteAll() {
        String query = "DELETE FROM " + TABLE_NAME;
        jdbcUtils.executePreparedQuery(query);
        logger.warn("Deleted all users from database");
    }

    // ==================== Custom Query Methods ====================

    @Override
    public Optional<User> findByEmail(String email) {
        String query = BASE_SELECT + " WHERE email = ?";
        List<User> users = jdbcUtils.query(query, userRowMapper, email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.getFirst());
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String query = BASE_SELECT + " WHERE username = ?";
        List<User> users = jdbcUtils.query(query, userRowMapper, username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public boolean existsByEmail(String email) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE email = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, email);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE username = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, username);
        return count != null && count > 0;
    }


    @Override
    public Long countByIsActive(boolean isActive) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE is_active = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, isActive);
        return count != null ? count : 0L;
    }



    @Override
    public Page<User> findByRole(String role, Pageable pageable) {
        // Count total
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE role = ?";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, role);
        long totalCount = total != null ? total : 0L;

        // Get paginated results
        String query = BASE_SELECT + " WHERE role = ?" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<User> users = jdbcUtils.query(query, userRowMapper,
                role, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(users, pageable, totalCount);
    }

    @Override
    public Page<User> findByIsActive(boolean isActive, Pageable pageable) {
        // Count total
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE is_active = ?";
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, isActive);
        long totalCount = total != null ? total : 0L;

        // Get paginated results
        String query = BASE_SELECT + " WHERE is_active = ?" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT ? OFFSET ?";

        List<User> users = jdbcUtils.query(query, userRowMapper,
                isActive, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(users, pageable, totalCount);
    }

    @Override
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        String likeKeyword = "%" + keyword.toLowerCase() + "%";

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", likeKeyword);

        // Count total
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                " WHERE LOWER(first_name) LIKE :keyword " +
                "OR LOWER(last_name) LIKE :keyword " +
                "OR LOWER(email) LIKE :keyword " +
                "OR LOWER(username) LIKE :keyword";

        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        // Get paginated results
        String query = BASE_SELECT +
                " WHERE LOWER(first_name) LIKE :keyword " +
                "OR LOWER(last_name) LIKE :keyword " +
                "OR LOWER(email) LIKE :keyword " +
                "OR LOWER(username) LIKE :keyword" +
                buildOrderByClause(pageable.getSort()) +
                " LIMIT :limit OFFSET :offset";

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<User> users = jdbcUtils.query(query, userRowMapper, params);

        return new PageImpl<>(users, pageable, totalCount);
    }

    @Override
    @Transactional
    public boolean updatePassword(Long userId, String newPassword) {
        Map<String, Object> params = new HashMap<>();
        params.put("password", newPassword);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", userId);

        String query = "UPDATE " + TABLE_NAME +
                " SET password = :password, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (!result.hasError() && result.getAffectedRows() > 0) {
            logger.info("Updated password for user id: {}", userId);
            return true;
        }
        return false;
    }


    @Override
    @Transactional
    public int batchUpdate(List<User> users) {
        if (users == null || users.isEmpty()) {
            return 0;
        }

        String query = "UPDATE " + TABLE_NAME +
                " SET email = :email, username = :username, password = :password, " +
                "first_name = :firstName, last_name = :lastName, phone_number = :phoneNumber, " +
                "role = :role, is_active = :isActive, updated_at = :updatedAt " +
                "WHERE id = :id";

        List<SqlParameterSource> batchParams = users.stream()
                .map(user -> {
                    user.setUpdatedAt(LocalDateTime.now());

                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("email", user.getEmail());
                    params.addValue("username", user.getUsername());
                    params.addValue("password", user.getPassword());
                    params.addValue("firstName", user.getFirstName());
                    params.addValue("lastName", user.getLastName());
                    params.addValue("phoneNumber", user.getPhoneNumber());
                    params.addValue("role", user.getRole() != null ? user.getRole().name() : null);
                    params.addValue("isActive", user.getIsActive());
                    params.addValue("updatedAt", Timestamp.valueOf(user.getUpdatedAt()));
                    params.addValue("id", user.getId());

                    return params;
                })
                .collect(Collectors.toList());

        int[] results = jdbcUtils.batchUpdate(query, batchParams.toArray(new SqlParameterSource[0]));
        int totalUpdated = Arrays.stream(results).sum();

        logger.info("Batch updated {} users", totalUpdated);
        return totalUpdated;
    }

    // ==================== Helper Methods ====================

    /**
     * Insert a new user
     */
    @Transactional
    private User insert(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("email", user.getEmail());
        params.put("username", user.getUsername());
        params.put("password", user.getPassword());
        params.put("firstName", user.getFirstName());
        params.put("lastName", user.getLastName());
        params.put("phoneNumber", user.getPhoneNumber());
        params.put("role", user.getRole() != null ? user.getRole().name() : null);
        params.put("isActive", user.getIsActive());
        params.put("createdAt", Timestamp.valueOf(user.getCreatedAt()));
        params.put("updatedAt", Timestamp.valueOf(user.getUpdatedAt()));

        String query = "INSERT INTO " + TABLE_NAME +
                " (email, username, password, first_name, last_name, phone_number, role, is_active, created_at, updated_at) " +
                "VALUES (:email, :username, :password, :firstName, :lastName, :phoneNumber, :role, :isActive, :createdAt, :updatedAt)";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (result.hasError()) {
            logger.error("Error inserting user: {} | Query: {} | Params: {}", result.getError(), query, params);
            throw new RuntimeException("User insert failed: " + result.getError());
        }

        if (result.getGeneratedKey() != null) {
            user.setId(result.getGeneratedKey());
        } else {
            logger.error("No generated key returned for user insert! Params: {}", params);
            throw new RuntimeException("User insert failed: No generated key returned.");
        }

        logger.info("Inserted user with id: {}", user.getId());
        return user;
    }

    /**
     * Update an existing user
     */
    @Transactional
    private User update(User user) {
        user.setUpdatedAt(LocalDateTime.now());

        Map<String, Object> params = new HashMap<>();
        params.put("email", user.getEmail());
        params.put("username", user.getUsername());
        params.put("password", user.getPassword());
        params.put("firstName", user.getFirstName());
        params.put("lastName", user.getLastName());
        params.put("phoneNumber", user.getPhoneNumber());
        params.put("role", user.getRole() != null ? user.getRole().name() : null);
        params.put("isActive", user.getIsActive());
        params.put("updatedAt", Timestamp.valueOf(user.getUpdatedAt()));
        params.put("id", user.getId());

        String query = "UPDATE " + TABLE_NAME +
                " SET email = :email, username = :username, password = :password, " +
                "first_name = :firstName, last_name = :lastName, phone_number = :phoneNumber, " +
                "role = :role, is_active = :isActive, updated_at = :updatedAt " +
                "WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (!result.hasError()) {
            logger.info("Updated user with id: {}", user.getId());
        } else {
            logger.error("Error updating user with id {}: {}", user.getId(), result.getError());
        }

        return user;
    }

    /**
     * Build ORDER BY clause from Sort object
     */
    private String buildOrderByClause(Sort sort) {
        if (sort == null || !sort.iterator().hasNext()) {
            return "";
        }

        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        Iterator<Sort.Order> iterator = sort.iterator();

        while (iterator.hasNext()) {
            Sort.Order order = iterator.next();
            orderBy.append(camelToSnake(order.getProperty()))
                    .append(" ")
                    .append(order.getDirection().name());

            if (iterator.hasNext()) {
                orderBy.append(", ");
            }
        }

        return orderBy.toString();
    }

    /**
     * Convert camelCase to snake_case for database column names
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}

