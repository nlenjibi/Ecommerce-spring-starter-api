package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils.QueryResult;
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
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    @Override
    @Transactional
    public List<User> saveAll(Iterable<User> users) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            result.add(save(user));
        }
        return result;
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

    @Override
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
        String query = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        QueryResult result = jdbcUtils.executePreparedQuery(query, id);
        if (!result.hasError()) {
            logger.info("Deleted user with id: {}", id);
        } else {
            logger.error("Error deleting user with id {}: {}", id, result.getError());
        }
    }

    @Override
    @Transactional
    public void delete(User user) {
        deleteById(user.getId());
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<Long> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<User> users) {
        users.forEach(this::delete);
    }

    @Override
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
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
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
    public Optional<User> findActiveById(Long id) {
        String query = BASE_SELECT + " WHERE is_active = true AND id = ?";
        List<User> users = jdbcUtils.query(query, userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public Page<User> findUsersWithFilters(String firstName, String lastName,
                                           String role, Pageable pageable) {
        StringBuilder queryBuilder = new StringBuilder(BASE_SELECT);
        queryBuilder.append(" WHERE 1=1");

        Map<String, Object> params = new HashMap<>();

        if (firstName != null && !firstName.isEmpty()) {
            queryBuilder.append(" AND LOWER(first_name) LIKE LOWER(:firstName)");
            params.put("firstName", "%" + firstName + "%");
        }

        if (lastName != null && !lastName.isEmpty()) {
            queryBuilder.append(" AND LOWER(last_name) LIKE LOWER(:lastName)");
            params.put("lastName", "%" + lastName + "%");
        }

        if (role != null && !role.isEmpty()) {
            queryBuilder.append(" AND role = :role");
            params.put("role", role);
        }

        // Count total
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_NAME +
                queryBuilder.substring(BASE_SELECT.length());
        Long total = jdbcUtils.queryForObject(countQuery, Long.class, params);
        long totalCount = total != null ? total : 0L;

        // Add pagination
        queryBuilder.append(buildOrderByClause(pageable.getSort()));
        queryBuilder.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<User> users = jdbcUtils.query(queryBuilder.toString(), userRowMapper, params);

        return new PageImpl<>(users, pageable, totalCount);
    }

    @Override
    public Long countByIsActive(boolean isActive) {
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE is_active = ?";
        Long count = jdbcUtils.queryForObject(query, Long.class, isActive);
        return count != null ? count : 0L;
    }

    @Override
    public List<User> findAllActiveUsers() {
        String query = BASE_SELECT + " WHERE is_active = true";
        return jdbcUtils.query(query, userRowMapper);
    }

    @Override
    public List<User> findByRole(String role) {
        String query = BASE_SELECT + " WHERE role = ?";
        return jdbcUtils.query(query, userRowMapper, role);
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
    public boolean updateActiveStatus(Long userId, boolean isActive) {
        Map<String, Object> params = new HashMap<>();
        params.put("isActive", isActive);
        params.put("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        params.put("id", userId);

        String query = "UPDATE " + TABLE_NAME +
                " SET is_active = :isActive, updated_at = :updatedAt WHERE id = :id";

        QueryResult result = jdbcUtils.executeNamedQuery(query, params);

        if (!result.hasError() && result.getAffectedRows() > 0) {
            logger.info("Updated active status to {} for user id: {}", isActive, userId);
            return true;
        }
        return false;
    }

    @Override
    public List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String query = BASE_SELECT + " WHERE created_at BETWEEN ? AND ?";
        return jdbcUtils.query(query, userRowMapper,
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
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

        if (result.getGeneratedKey() != null) {
            user.setId(result.getGeneratedKey());
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