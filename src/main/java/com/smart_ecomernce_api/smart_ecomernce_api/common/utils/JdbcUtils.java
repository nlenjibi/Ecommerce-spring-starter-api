package com.smart_ecomernce_api.smart_ecomernce_api.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;

import java.sql.Statement;
import java.util.*;

/**
 * JdbcUtils provides utility methods for JDBC operations using Spring JDBC
 * Simplifies database operations with JdbcTemplate and NamedParameterJdbcTemplate
 */
@Component
public class JdbcUtils {
    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcUtils(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        // Configure JdbcTemplate
        this.jdbcTemplate.setFetchSize(100);
        this.jdbcTemplate.setQueryTimeout(30); // seconds
    }

    /**
     * Execute a prepared statement query with parameters
     * @param query SQL query with ? placeholders
     * @param params Array of parameters to bind to the query
     * @return QueryResult object containing results or affected rows
     */
    public QueryResult executePreparedQuery(String query, Object... params) {
        try {
            logger.info("Executing Query: {}", query);
            logger.debug("Query Parameters: {}", Arrays.toString(params));

            String queryType = query.trim().substring(0, Math.min(6, query.trim().length())).toLowerCase();

            if (queryType.startsWith("select") || queryType.startsWith("show")) {
                return executeSelectQuery(query, params);
            } else if (queryType.startsWith("insert")) {
                return executeInsertQuery(query, params);
            } else {
                return executeUpdateQuery(query, params);
            }

        } catch (DataAccessException e) {
            logger.error("Database Query Error: {} | Query: {} | Parameters: {}",
                    e.getMessage(), query, Arrays.toString(params), e);
            return new QueryResult("Database error: " + e.getMessage());
        }
    }

    /**
     * Execute a query with named parameters
     * @param query SQL query with :paramName placeholders
     * @param paramMap Map of parameter names to values
     * @return QueryResult object containing results or affected rows
     */
    public QueryResult executeNamedQuery(String query, Map<String, Object> paramMap) {
        try {
            logger.info("Executing Named Query: {}", query);
            logger.debug("Query Parameters: {}", paramMap);

            String queryType = query.trim().substring(0, Math.min(6, query.trim().length())).toLowerCase();

            if (queryType.startsWith("select") || queryType.startsWith("show")) {
                return executeNamedSelectQuery(query, paramMap);
            } else if (queryType.startsWith("insert")) {
                return executeNamedInsertQuery(query, paramMap);
            } else {
                return executeNamedUpdateQuery(query, paramMap);
            }

        } catch (DataAccessException e) {
            logger.error("Database Named Query Error: {} | Query: {} | Parameters: {}",
                    e.getMessage(), query, paramMap, e);
            return new QueryResult("Database error: " + e.getMessage());
        }
    }

    /**
     * Execute SELECT query with positional parameters
     */
    private QueryResult executeSelectQuery(String query, Object... params) {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query, params);
            return new QueryResult(result);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return new QueryResult(new ArrayList<>());
        }
    }

    /**
     * Execute SELECT query with named parameters
     */
    private QueryResult executeNamedSelectQuery(String query, Map<String, Object> paramMap) {
        try {
            List<Map<String, Object>> result = namedParameterJdbcTemplate.queryForList(query, paramMap);
            return new QueryResult(result);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return new QueryResult(new ArrayList<>());
        }
    }

    /**
     * Execute INSERT query with positional parameters and return generated key
     */
    private QueryResult executeInsertQuery(String query, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int affectedRows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("GENERATED_KEY")) {
            Number key = keyHolder.getKey();
            return key != null ?
                    new QueryResult(key.longValue(), affectedRows) :
                    new QueryResult(affectedRows);
        }

        return new QueryResult(affectedRows);
    }

    /**
     * Execute INSERT query with named parameters and return generated key
     */
    private QueryResult executeNamedInsertQuery(String query, Map<String, Object> paramMap) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource parameters = new MapSqlParameterSource(paramMap);

        int affectedRows = namedParameterJdbcTemplate.update(
                query,
                parameters,
                keyHolder,
                new String[]{"id"} // Specify column names for generated keys
        );

        if (keyHolder.getKey() != null) {
            return new QueryResult(keyHolder.getKey().longValue(), affectedRows);
        }

        return new QueryResult(affectedRows);
    }

    /**
     * Execute UPDATE/DELETE query with positional parameters
     */
    private QueryResult executeUpdateQuery(String query, Object... params) {
        int affectedRows = jdbcTemplate.update(query, params);
        return new QueryResult(affectedRows);
    }

    /**
     * Execute UPDATE/DELETE query with named parameters
     */
    private QueryResult executeNamedUpdateQuery(String query, Map<String, Object> paramMap) {
        int affectedRows = namedParameterJdbcTemplate.update(query, paramMap);
        return new QueryResult(affectedRows);
    }

    /**
     * Query for a single result row
     */
    public Map<String, Object> queryForMap(String query, Object... params) {
        try {
            return jdbcTemplate.queryForMap(query, params);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return Collections.emptyMap();
        }
    }

    /**
     * Query for a single result row with named parameters
     */
    public Map<String, Object> queryForMap(String query, Map<String, Object> paramMap) {
        try {
            return namedParameterJdbcTemplate.queryForMap(query, paramMap);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return Collections.emptyMap();
        }
    }

    /**
     * Query for a single value
     */
    public <T> T queryForObject(String query, Class<T> requiredType, Object... params) {
        try {
            return jdbcTemplate.queryForObject(query, requiredType, params);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return null;
        }
    }

    /**
     * Query for a single value with named parameters
     */
    public <T> T queryForObject(String query, Class<T> requiredType, Map<String, Object> paramMap) {
        try {
            return namedParameterJdbcTemplate.queryForObject(query, paramMap, requiredType);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("No results found for query: {}", query);
            return null;
        }
    }

    /**
     * Query with custom RowMapper
     */
    public <T> List<T> query(String query, RowMapper<T> rowMapper, Object... params) {
        return jdbcTemplate.query(query, rowMapper, params);
    }

    /**
     * Query with custom RowMapper and named parameters
     */
    public <T> List<T> query(String query, RowMapper<T> rowMapper, Map<String, Object> paramMap) {
        return namedParameterJdbcTemplate.query(query, paramMap, rowMapper);
    }

    /**
     * Execute batch update with positional parameters
     */
    public int[] batchUpdate(String query, List<Object[]> batchArgs) {
        return jdbcTemplate.batchUpdate(query, batchArgs);
    }

    /**
     * Execute batch update with named parameters
     */
    public int[] batchUpdate(String query, SqlParameterSource[] batchArgs) {
        return namedParameterJdbcTemplate.batchUpdate(query, batchArgs);
    }

    /**
     * Execute within transaction
     */
    @Transactional
    public void executeInTransaction(Runnable operation) {
        operation.run();
    }

    /**
     * Execute multiple operations in transaction
     */
    @Transactional
    public void executeInTransaction(List<Runnable> operations) {
        for (Runnable operation : operations) {
            operation.run();
        }
    }

    /**
     * Inner class to hold query results
     */
    public static class QueryResult {
        private List<Map<String, Object>> resultSet;
        private Long generatedKey;
        private Integer affectedRows;
        private String error;

        // Constructor for SELECT queries
        public QueryResult(List<Map<String, Object>> resultSet) {
            this.resultSet = resultSet;
        }

        // Constructor for INSERT queries with generated key
        public QueryResult(Long generatedKey, Integer affectedRows) {
            this.generatedKey = generatedKey;
            this.affectedRows = affectedRows;
        }

        // Constructor for UPDATE/DELETE queries (affected rows)
        public QueryResult(Integer affectedRows) {
            this.affectedRows = affectedRows;
        }

        // Constructor for errors
        public QueryResult(String error) {
            this.error = error;
        }

        public List<Map<String, Object>> getResultSet() {
            return resultSet != null ? resultSet : Collections.emptyList();
        }

        public Long getGeneratedKey() {
            return generatedKey;
        }

        public Integer getAffectedRows() {
            return affectedRows != null ? affectedRows : 0;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return error != null;
        }

        public boolean hasResults() {
            return resultSet != null && !resultSet.isEmpty();
        }

        public Optional<Map<String, Object>> getFirstResult() {
            return hasResults() ? Optional.of(resultSet.get(0)) : Optional.empty();
        }
    }

    /**
     * Utility method to create BeanPropertyRowMapper
     */
    public static <T> RowMapper<T> getBeanRowMapper(Class<T> mappedClass) {
        return new BeanPropertyRowMapper<>(mappedClass);
    }

    /**
     * Utility method to create ColumnMapRowMapper
     */
    public static RowMapper<Map<String, Object>> getColumnMapRowMapper() {
        return new ColumnMapRowMapper();
    }

    /**
     * Get JdbcTemplate instance for advanced operations
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * Get NamedParameterJdbcTemplate instance
     */
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }
}