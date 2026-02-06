package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.ReviewRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC implementation of ReviewRepository using JdbcUtils
 * Provides high-performance database operations for reviews
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewRepositoryImpl implements ReviewRepository {

    private final JdbcUtils jdbcUtils;

    // ==================== Row Mappers ====================

    private final RowMapper<Review> reviewRowMapper = (rs, rowNum) -> {
        Review review = Review.builder()
                .id(rs.getLong("id"))
                .rating(rs.getInt("rating"))
                .title(rs.getString("title"))
                .comment(rs.getString("comment"))
                .verifiedPurchase(rs.getBoolean("verified_purchase"))
                .approved(rs.getBoolean("approved"))
                .helpfulCount(rs.getInt("helpful_count"))
                .notHelpfulCount(rs.getInt("not_helpful_count"))
                .adminResponse(rs.getString("admin_response"))
                .rejectionReason(rs.getString("rejection_reason"))
                .deleted(rs.getBoolean("deleted"))
                .build();

        // Set timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) review.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) review.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp adminResponseAt = rs.getTimestamp("admin_response_at");
        if (adminResponseAt != null) review.setAdminResponseAt(adminResponseAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) review.setDeletedAt(deletedAt.toLocalDateTime());

        // Set admin response by
        Long adminResponseBy = (Long) rs.getObject("admin_response_by");
        if (adminResponseBy != null) review.setAdminResponseBy(adminResponseBy);

        // Load product (lazy loading simulation)
        Long productId = rs.getLong("product_id");
        Product product = new Product();
        product.setId(productId);
        review.setProduct(product);

        // Load user (lazy loading simulation)
        Long userId = rs.getLong("user_id");
        User user = new User();
        user.setId(userId);
        review.setUser(user);

        // Load collections
        review.setImages(loadReviewImages(review.getId()));
        review.setPros(loadReviewPros(review.getId()));
        review.setCons(loadReviewCons(review.getId()));

        return review;
    };

    private final RowMapper<Review> reviewWithUserProductRowMapper = (rs, rowNum) -> {
        Review review = Review.builder()
                .id(rs.getLong("id"))
                .rating(rs.getInt("rating"))
                .title(rs.getString("title"))
                .comment(rs.getString("comment"))
                .verifiedPurchase(rs.getBoolean("verified_purchase"))
                .approved(rs.getBoolean("approved"))
                .helpfulCount(rs.getInt("helpful_count"))
                .notHelpfulCount(rs.getInt("not_helpful_count"))
                .adminResponse(rs.getString("admin_response"))
                .rejectionReason(rs.getString("rejection_reason"))
                .deleted(rs.getBoolean("deleted"))
                .build();

        // Set timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) review.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) review.setUpdatedAt(updatedAt.toLocalDateTime());
        Timestamp adminResponseAt = rs.getTimestamp("admin_response_at");
        if (adminResponseAt != null) review.setAdminResponseAt(adminResponseAt.toLocalDateTime());
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) review.setDeletedAt(deletedAt.toLocalDateTime());
        Long adminResponseBy = (Long) rs.getObject("admin_response_by");
        if (adminResponseBy != null) review.setAdminResponseBy(adminResponseBy);

        // Load product info from join
        Product product = new Product();
        product.setId(rs.getLong("product_id"));
        product.setName(rs.getString("product_name"));
        product.setSlug(rs.getString("product_slug"));
        review.setProduct(product);

        // Load user info from join
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setFirstName(rs.getString("user_first_name"));
        user.setLastName(rs.getString("user_last_name"));
        user.setEmail(rs.getString("user_email"));
        review.setUser(user);

        // Load collections
        review.setImages(loadReviewImages(review.getId()));
        review.setPros(loadReviewPros(review.getId()));
        review.setCons(loadReviewCons(review.getId()));

        return review;
    };

    // ==================== Helper Methods ====================

    private List<String> loadReviewImages(Long reviewId) {
        String sql = "SELECT image_url FROM review_images WHERE review_id = ? ORDER BY image_order";
        return jdbcUtils.query(sql, (rs, rowNum) -> rs.getString("image_url"), reviewId);
    }

    private List<String> loadReviewPros(Long reviewId) {
        String sql = "SELECT pro FROM review_pros WHERE review_id = ? ORDER BY pro_order";
        return jdbcUtils.query(sql, (rs, rowNum) -> rs.getString("pro"), reviewId);
    }

    private List<String> loadReviewCons(Long reviewId) {
        String sql = "SELECT con FROM review_cons WHERE review_id = ? ORDER BY con_order";
        return jdbcUtils.query(sql, (rs, rowNum) -> rs.getString("con"), reviewId);
    }

    private void saveReviewCollections(Review review) {
        Long reviewId = review.getId();

        // Delete existing collections
        jdbcUtils.executePreparedQuery("DELETE FROM review_images WHERE review_id = ?", reviewId);
        jdbcUtils.executePreparedQuery("DELETE FROM review_pros WHERE review_id = ?", reviewId);
        jdbcUtils.executePreparedQuery("DELETE FROM review_cons WHERE review_id = ?", reviewId);

        // Save images
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            String sql = "INSERT INTO review_images (review_id, image_url, image_order) VALUES (?, ?, ?)";
            for (int i = 0; i < review.getImages().size(); i++) {
                jdbcUtils.executePreparedQuery(sql, reviewId, review.getImages().get(i), i);
            }
        }

        // Save pros
        if (review.getPros() != null && !review.getPros().isEmpty()) {
            String sql = "INSERT INTO review_pros (review_id, pro, pro_order) VALUES (?, ?, ?)";
            for (int i = 0; i < review.getPros().size(); i++) {
                jdbcUtils.executePreparedQuery(sql, reviewId, review.getPros().get(i), i);
            }
        }

        // Save cons
        if (review.getCons() != null && !review.getCons().isEmpty()) {
            String sql = "INSERT INTO review_cons (review_id, con, con_order) VALUES (?, ?, ?)";
            for (int i = 0; i < review.getCons().size(); i++) {
                jdbcUtils.executePreparedQuery(sql, reviewId, review.getCons().get(i), i);
            }
        }
    }

    // ==================== Basic CRUD ====================

    @Override
    public Review save(Review review) {
        if (review.getId() == null) {
            return insert(review);
        } else {
            return update(review);
        }
    }

    private Review insert(Review review) {
        String sql = """
            INSERT INTO reviews (
                product_id, user_id, rating, title, comment,
                verified_purchase, approved, helpful_count, not_helpful_count,
                admin_response, admin_response_at, admin_response_by,
                rejection_reason, deleted, deleted_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getVerifiedPurchase(),
                review.getApproved(),
                review.getHelpfulCount(),
                review.getNotHelpfulCount(),
                review.getAdminResponse(),
                review.getAdminResponseAt() != null ? Timestamp.valueOf(review.getAdminResponseAt()) : null,
                review.getAdminResponseBy(),
                review.getRejectionReason(),
                review.getDeleted(),
                review.getDeletedAt() != null ? Timestamp.valueOf(review.getDeletedAt()) : null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );

        if (result.getGeneratedKey() != null) {
            review.setId(result.getGeneratedKey());
        }

        saveReviewCollections(review);
        return review;
    }

    private Review update(Review review) {
        String sql = """
            UPDATE reviews SET
                rating = ?, title = ?, comment = ?,
                verified_purchase = ?, approved = ?,
                helpful_count = ?, not_helpful_count = ?,
                admin_response = ?, admin_response_at = ?, admin_response_by = ?,
                rejection_reason = ?, deleted = ?, deleted_at = ?,
                updated_at = ?
            WHERE id = ?
        """;

        jdbcUtils.executePreparedQuery(sql,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getVerifiedPurchase(),
                review.getApproved(),
                review.getHelpfulCount(),
                review.getNotHelpfulCount(),
                review.getAdminResponse(),
                review.getAdminResponseAt() != null ? Timestamp.valueOf(review.getAdminResponseAt()) : null,
                review.getAdminResponseBy(),
                review.getRejectionReason(),
                review.getDeleted(),
                review.getDeletedAt() != null ? Timestamp.valueOf(review.getDeletedAt()) : null,
                Timestamp.valueOf(LocalDateTime.now()),
                review.getId()
        );

        saveReviewCollections(review);
        return review;
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = "SELECT * FROM reviews WHERE id = ? AND deleted = false";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper, id);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    @Override
    public Optional<Review> findByIdIncludingDeleted(Long id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper, id);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "UPDATE reviews SET deleted = true, deleted_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean hardDeleteById(Long id) {
        // Delete collections first
        jdbcUtils.executePreparedQuery("DELETE FROM review_images WHERE review_id = ?", id);
        jdbcUtils.executePreparedQuery("DELETE FROM review_pros WHERE review_id = ?", id);
        jdbcUtils.executePreparedQuery("DELETE FROM review_cons WHERE review_id = ?", id);

        String sql = "DELETE FROM reviews WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean restoreById(Long id) {
        String sql = "UPDATE reviews SET deleted = false, deleted_at = NULL WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE id = ? AND deleted = false";
        Integer count = jdbcUtils.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByProductIdAndUserId(Long productId, Long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND user_id = ? AND deleted = false";
        Integer count = jdbcUtils.queryForObject(sql, Integer.class, productId, userId);
        return count != null && count > 0;
    }

    // ==================== Query Methods ====================

    @Override
    public Page<Review> findAll(Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class);

        String sql = "SELECT * FROM reviews WHERE deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByProductId(Long productId, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId);

        String sql = "SELECT * FROM reviews WHERE product_id = ? AND deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByProductIdAndApproved(Long productId, Boolean approved, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND approved = ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId, approved);

        String sql = """
                SELECT r.id, r.product_id, r.user_id, r.rating, r.title, r.comment, 
                       r.verified_purchase, r.approved, r.helpful_count, r.not_helpful_count, 
                       r.admin_response, r.admin_response_at, r.admin_response_by, 
                       r.rejection_reason, r.deleted, r.deleted_at, r.created_at, r.updated_at,
                       p.name AS product_name, p.slug AS product_slug,
                       u.first_name AS user_first_name, u.last_name AS user_last_name, u.email AS user_email
                FROM reviews r
                LEFT JOIN products p ON r.product_id = p.id
                LEFT JOIN users u ON r.user_id = u.id
                WHERE r.product_id = ? AND r.approved = ? AND r.deleted = false
                ORDER BY r.created_at DESC
                LIMIT ? OFFSET ?
                """;

        List<Review> reviews = jdbcUtils.query(sql, reviewWithUserProductRowMapper,
                productId, approved, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND verified_purchase = ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId, verifiedPurchase);

        String sql = "SELECT * FROM reviews WHERE product_id = ? AND verified_purchase = ? AND deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, verifiedPurchase, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByUserId(Long userId, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, userId);

        String sql = "SELECT * FROM reviews WHERE user_id = ? AND deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                userId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByProductIdAndRating(Long productId, Integer rating, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND rating = ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId, rating);

        String sql = "SELECT * FROM reviews WHERE product_id = ? AND rating = ? AND deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, rating, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findPendingReviews(Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE approved = false AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class);

        String sql = """
                SELECT r.id, r.product_id, r.user_id, r.rating, r.title, r.comment,
                       r.verified_purchase, r.approved, r.helpful_count, r.not_helpful_count,
                       r.admin_response, r.admin_response_at, r.admin_response_by,
                       r.rejection_reason, r.deleted, r.deleted_at, r.created_at, r.updated_at,
                       u.first_name AS user_first_name, u.last_name AS user_last_name, u.email AS user_email,
                       p.name AS product_name, p.slug AS product_slug
                FROM reviews r
                JOIN users u ON r.user_id = u.id
                JOIN products p ON r.product_id = p.id
                WHERE r.approved = false AND r.deleted = false
                ORDER BY r.created_at DESC LIMIT ? OFFSET ?
                """;
        List<Review> reviews = jdbcUtils.query(sql, reviewWithUserProductRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public List<Review> findMostHelpfulReviews(Long productId, int limit) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND approved = true AND deleted = false 
            ORDER BY (helpful_count - not_helpful_count) DESC, helpful_count DESC 
            LIMIT ?
        """;
        return jdbcUtils.query(sql, reviewRowMapper, productId, limit);
    }

    @Override
    public List<Review> findRecentReviews(Long productId, int limit) {
        String sql = "SELECT * FROM reviews WHERE product_id = ? AND approved = true AND deleted = false ORDER BY created_at DESC LIMIT ?";
        return jdbcUtils.query(sql, reviewRowMapper, productId, limit);
    }

    @Override
    public Page<Review> findReviewsWithImages(Long productId, Pageable pageable) {
        String countSql = """
            SELECT COUNT(DISTINCT r.id) FROM reviews r
            INNER JOIN review_images ri ON r.id = ri.review_id
            WHERE r.product_id = ? AND r.deleted = false
        """;
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId);

        String sql = """
            SELECT DISTINCT r.* FROM reviews r
            INNER JOIN review_images ri ON r.id = ri.review_id
            WHERE r.product_id = ? AND r.deleted = false
            ORDER BY r.created_at DESC
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByProductIdAndDateRange(Long productId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND created_at BETWEEN ? AND ? AND deleted = false";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId,
                Timestamp.valueOf(from), Timestamp.valueOf(to));

        String sql = "SELECT * FROM reviews WHERE product_id = ? AND created_at BETWEEN ? AND ? AND deleted = false ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, Timestamp.valueOf(from), Timestamp.valueOf(to),
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> findByFilters(Long productId, Integer rating, Boolean verifiedPurchase,
                                      Boolean approved, Boolean withImages,
                                      LocalDateTime dateFrom, LocalDateTime dateTo,
                                      Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);
        params.put("deleted", false);

        StringBuilder whereClause = new StringBuilder("WHERE product_id = :productId AND deleted = :deleted");

        if (rating != null) {
            whereClause.append(" AND rating = :rating");
            params.put("rating", rating);
        }
        if (verifiedPurchase != null) {
            whereClause.append(" AND verified_purchase = :verifiedPurchase");
            params.put("verifiedPurchase", verifiedPurchase);
        }
        if (approved != null) {
            whereClause.append(" AND approved = :approved");
            params.put("approved", approved);
        }
        if (dateFrom != null) {
            whereClause.append(" AND created_at >= :dateFrom");
            params.put("dateFrom", Timestamp.valueOf(dateFrom));
        }
        if (dateTo != null) {
            whereClause.append(" AND created_at <= :dateTo");
            params.put("dateTo", Timestamp.valueOf(dateTo));
        }

        String baseQuery = withImages != null && withImages ?
                "SELECT DISTINCT r.* FROM reviews r INNER JOIN review_images ri ON r.id = ri.review_id " :
                "SELECT * FROM reviews ";

        String countSql = "SELECT COUNT(*) FROM (" + baseQuery + whereClause + ") as count_query";
        Long total = jdbcUtils.queryForObject(countSql, Long.class, params);

        String sql = baseQuery + whereClause + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper, params);

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    // ==================== Count Methods ====================

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM reviews WHERE deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public long countByProductId(Long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId);
        return count != null ? count : 0;
    }

    @Override
    public long countByProductIdAndApproved(Long productId, Boolean approved) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND approved = ? AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId, approved);
        return count != null ? count : 0;
    }

    @Override
    public long countByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND verified_purchase = ? AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId, verifiedPurchase);
        return count != null ? count : 0;
    }

    @Override
    public long countPendingReviews() {
        String sql = "SELECT COUNT(*) FROM reviews WHERE approved = false AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0;
    }

    @Override
    public long countByProductIdAndRating(Long productId, Integer rating) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND rating = ? AND deleted = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId, rating);
        return count != null ? count : 0;
    }

    @Override
    public Double getAverageRatingByProductId(Long productId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE product_id = ? AND approved = true AND deleted = false";
        return jdbcUtils.queryForObject(sql, Double.class, productId);
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(Long productId) {
        String sql = """
            SELECT rating, COUNT(*) as count 
            FROM reviews 
            WHERE product_id = ? AND approved = true AND deleted = false 
            GROUP BY rating
        """;

        Map<Integer, Long> distribution = new HashMap<>();
        // Initialize with zeros
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Map<String, Object>> results = jdbcUtils.executePreparedQuery(sql, productId).getResultSet();
        for (Map<String, Object> row : results) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            distribution.put(rating, count);
        }

        return distribution;
    }

    @Override
    public Map<Integer, Map<String, Object>> getRatingDistributionWithPercentages(Long productId) {
        Map<Integer, Long> distribution = getRatingDistribution(productId);
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();

        Map<Integer, Map<String, Object>> result = new HashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            long count = distribution.get(rating);
            double percentage = total > 0 ? (count * 100.0) / total : 0.0;

            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            data.put("percentage", percentage);
            result.put(rating, data);
        }

        return result;
    }

    @Override
    public Map<String, Object> getProductRatingStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();

        Double avgRating = getAverageRatingByProductId(productId);
        long totalReviews = countByProductIdAndApproved(productId, true);
        long verifiedPurchases = countByProductIdAndVerifiedPurchase(productId, true);
        Map<Integer, Long> distribution = getRatingDistribution(productId);

        stats.put("productId", productId);
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        stats.put("totalReviews", totalReviews);
        stats.put("verifiedPurchases", verifiedPurchases);
        stats.put("verifiedPercentage", totalReviews > 0 ? (verifiedPurchases * 100.0) / totalReviews : 0.0);
        stats.put("distribution", distribution);

        return stats;
    }

    @Override
    public Map<String, Object> getUserReviewStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                COUNT(*) as total_reviews,
                AVG(rating) as avg_rating,
                SUM(helpful_count) as total_helpful,
                SUM(not_helpful_count) as total_not_helpful,
                COUNT(CASE WHEN verified_purchase = true THEN 1 END) as verified_count,
                COUNT(CASE WHEN approved = true THEN 1 END) as approved_count
            FROM reviews
            WHERE user_id = ? AND deleted = false
        """;

        Map<String, Object> result = jdbcUtils.queryForMap(sql, userId);
        if (!result.isEmpty()) {
            stats.put("userId", userId);
            stats.put("totalReviews", ((Number) result.get("total_reviews")).longValue());
            stats.put("averageRating", result.get("avg_rating") != null ? ((Number) result.get("avg_rating")).doubleValue() : 0.0);
            stats.put("totalHelpfulVotes", ((Number) result.get("total_helpful")).longValue());
            stats.put("totalNotHelpfulVotes", ((Number) result.get("total_not_helpful")).longValue());
            stats.put("verifiedPurchases", ((Number) result.get("verified_count")).longValue());
            stats.put("approvedReviews", ((Number) result.get("approved_count")).longValue());
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getTopRatedProducts(int limit) {
        String sql = """
            SELECT 
                product_id,
                AVG(rating) as avg_rating,
                COUNT(*) as review_count
            FROM reviews
            WHERE approved = true AND deleted = false
            GROUP BY product_id
            HAVING COUNT(*) >= 5
            ORDER BY avg_rating DESC, review_count DESC
            LIMIT ?
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> product = new HashMap<>();
            product.put("productId", rs.getLong("product_id"));
            product.put("averageRating", rs.getDouble("avg_rating"));
            product.put("reviewCount", rs.getLong("review_count"));
            return product;
        }, limit);
    }

    @Override
    public List<Map<String, Object>> getMostReviewedProducts(int limit) {
        String sql = """
            SELECT 
                product_id,
                COUNT(*) as review_count,
                AVG(rating) as avg_rating
            FROM reviews
            WHERE approved = true AND deleted = false
            GROUP BY product_id
            ORDER BY review_count DESC, avg_rating DESC
            LIMIT ?
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> product = new HashMap<>();
            product.put("productId", rs.getLong("product_id"));
            product.put("reviewCount", rs.getLong("review_count"));
            product.put("averageRating", rs.getDouble("avg_rating"));
            return product;
        }, limit);
    }

    @Override
    public List<Map<String, Object>> getReviewTrendsOverTime(Long productId, int months) {
        String sql = """
            SELECT 
                DATE_TRUNC('month', created_at) as month,
                COUNT(*) as review_count
            FROM reviews
            WHERE product_id = ? 
                AND approved = true 
                AND deleted = false
                AND created_at >= NOW() - INTERVAL ? MONTH
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY month DESC
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> trend = new HashMap<>();
            trend.put("month", rs.getDate("month"));
            trend.put("reviewCount", rs.getLong("review_count"));
            return trend;
        }, productId, months);
    }

    @Override
    public List<Map<String, Object>> getAverageRatingTrend(Long productId, int months) {
        String sql = """
            SELECT 
                DATE_TRUNC('month', created_at) as month,
                AVG(rating) as avg_rating,
                COUNT(*) as review_count
            FROM reviews
            WHERE product_id = ? 
                AND approved = true 
                AND deleted = false
                AND created_at >= NOW() - INTERVAL ? MONTH
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY month DESC
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> trend = new HashMap<>();
            trend.put("month", rs.getDate("month"));
            trend.put("averageRating", rs.getDouble("avg_rating"));
            trend.put("reviewCount", rs.getLong("review_count"));
            return trend;
        }, productId, months);
    }

    // ==================== Update Operations ====================

    @Override
    public boolean approveReview(Long reviewId) {
        String sql = "UPDATE reviews SET approved = true, rejection_reason = NULL, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean rejectReview(Long reviewId, String reason) {
        String sql = "UPDATE reviews SET approved = false, rejection_reason = ?, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                reason, Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean incrementHelpfulCount(Long reviewId) {
        String sql = "UPDATE reviews SET helpful_count = helpful_count + 1, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean incrementNotHelpfulCount(Long reviewId) {
        String sql = "UPDATE reviews SET not_helpful_count = not_helpful_count + 1, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean addAdminResponse(Long reviewId, String response, Long adminId) {
        String sql = """
            UPDATE reviews 
            SET admin_response = ?, 
                admin_response_at = ?, 
                admin_response_by = ?,
                updated_at = ?
            WHERE id = ?
        """;
        LocalDateTime now = LocalDateTime.now();
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                response, Timestamp.valueOf(now), adminId, Timestamp.valueOf(now), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean removeAdminResponse(Long reviewId) {
        String sql = """
            UPDATE reviews 
            SET admin_response = NULL, 
                admin_response_at = NULL, 
                admin_response_by = NULL,
                updated_at = ?
            WHERE id = ?
        """;
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean updateVerificationStatus(Long reviewId, Boolean verified) {
        String sql = "UPDATE reviews SET verified_purchase = ?, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                verified, Timestamp.valueOf(LocalDateTime.now()), reviewId);
        return result.getAffectedRows() > 0;
    }

    // ==================== Bulk Operations ====================

    @Override
    public int deleteByProductId(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE reviews SET deleted = true, deleted_at = ? WHERE product_id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(now), productId);
        return result.getAffectedRows();
    }

    @Override
    public int deleteByUserId(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "UPDATE reviews SET deleted = true, deleted_at = ? WHERE user_id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(now), userId);
        return result.getAffectedRows();
    }

    @Override
    public int approveReviews(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) return 0;

        Map<String, Object> params = new HashMap<>();
        params.put("ids", reviewIds);
        params.put("now", Timestamp.valueOf(LocalDateTime.now()));

        String sql = "UPDATE reviews SET approved = true, rejection_reason = NULL, updated_at = :now WHERE id IN (:ids)";
        JdbcUtils.QueryResult result = jdbcUtils.executeNamedQuery(sql, params);
        return result.getAffectedRows();
    }

    @Override
    public int rejectReviews(List<Long> reviewIds, String reason) {
        if (reviewIds == null || reviewIds.isEmpty()) return 0;

        Map<String, Object> params = new HashMap<>();
        params.put("ids", reviewIds);
        params.put("reason", reason);
        params.put("now", Timestamp.valueOf(LocalDateTime.now()));

        String sql = "UPDATE reviews SET approved = false, rejection_reason = :reason, updated_at = :now WHERE id IN (:ids)";
        JdbcUtils.QueryResult result = jdbcUtils.executeNamedQuery(sql, params);
        return result.getAffectedRows();
    }

    @Override
    public int updateVerificationStatusFromOrders(Long productId) {
        String sql = """
            UPDATE reviews r
            SET verified_purchase = true, updated_at = ?
            WHERE r.product_id = ?
            AND EXISTS (
                SELECT 1 FROM order_items oi
                INNER JOIN orders o ON oi.order_id = o.id
                WHERE oi.product_id = r.product_id
                AND o.user_id = r.user_id
                AND o.status = 'DELIVERED'
            )
        """;
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                Timestamp.valueOf(LocalDateTime.now()), productId);
        return result.getAffectedRows();
    }

    // ==================== Search & Filter ====================

    @Override
    public Page<Review> searchReviews(String keyword, Pageable pageable) {
        String searchPattern = "%" + keyword.toLowerCase() + "%";

        String countSql = """
            SELECT COUNT(*) FROM reviews 
            WHERE (LOWER(title) LIKE ? OR LOWER(comment) LIKE ?) 
            AND deleted = false
        """;
        Long total = jdbcUtils.queryForObject(countSql, Long.class, searchPattern, searchPattern);

        String sql = """
            SELECT * FROM reviews 
            WHERE (LOWER(title) LIKE ? OR LOWER(comment) LIKE ?) 
            AND deleted = false 
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                searchPattern, searchPattern, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> searchReviewsByProduct(Long productId, String keyword, Pageable pageable) {
        String searchPattern = "%" + keyword.toLowerCase() + "%";

        String countSql = """
            SELECT COUNT(*) FROM reviews 
            WHERE product_id = ? 
            AND (LOWER(title) LIKE ? OR LOWER(comment) LIKE ?) 
            AND deleted = false
        """;
        Long total = jdbcUtils.queryForObject(countSql, Long.class, productId, searchPattern, searchPattern);

        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? 
            AND (LOWER(title) LIKE ? OR LOWER(comment) LIKE ?) 
            AND deleted = false 
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                productId, searchPattern, searchPattern, pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Review> getReviewsNeedingModeration(Pageable pageable) {
        return findPendingReviews(pageable);
    }

    @Override
    public Page<Review> getFlaggedReviews(Pageable pageable) {
        String countSql = """
            SELECT COUNT(*) FROM reviews 
            WHERE not_helpful_count > helpful_count * 2 
            AND deleted = false
        """;
        Long total = jdbcUtils.queryForObject(countSql, Long.class);

        String sql = """
            SELECT * FROM reviews 
            WHERE not_helpful_count > helpful_count * 2 
            AND deleted = false 
            ORDER BY not_helpful_count DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, reviewRowMapper,
                pageable.getPageSize(), pageable.getOffset());

        return new PageImpl<>(reviews, pageable, total != null ? total : 0);
    }

    // ==================== Analytics Methods ====================

    @Override
    public List<Map<String, Object>> getMostCommonPros(Long productId, int limit) {
        String sql = """
            SELECT pro, COUNT(*) as frequency
            FROM review_pros rp
            INNER JOIN reviews r ON rp.review_id = r.id
            WHERE r.product_id = ? AND r.approved = true AND r.deleted = false
            GROUP BY pro
            ORDER BY frequency DESC
            LIMIT ?
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> pro = new HashMap<>();
            pro.put("text", rs.getString("pro"));
            pro.put("frequency", rs.getLong("frequency"));
            return pro;
        }, productId, limit);
    }

    @Override
    public List<Map<String, Object>> getMostCommonCons(Long productId, int limit) {
        String sql = """
            SELECT con, COUNT(*) as frequency
            FROM review_cons rc
            INNER JOIN reviews r ON rc.review_id = r.id
            WHERE r.product_id = ? AND r.approved = true AND r.deleted = false
            GROUP BY con
            ORDER BY frequency DESC
            LIMIT ?
        """;

        return jdbcUtils.query(sql, (rs, rowNum) -> {
            Map<String, Object> con = new HashMap<>();
            con.put("text", rs.getString("con"));
            con.put("frequency", rs.getLong("frequency"));
            return con;
        }, productId, limit);
    }

    @Override
    public Map<String, Object> getReviewVelocity(Long productId) {
        Map<String, Object> velocity = new HashMap<>();

        String sql = """
            SELECT 
                COUNT(CASE WHEN created_at >= NOW() - INTERVAL 1 DAY THEN 1 END) as daily,
                COUNT(CASE WHEN created_at >= NOW() - INTERVAL 7 DAY THEN 1 END) as weekly,
                COUNT(CASE WHEN created_at >= NOW() - INTERVAL 30 DAY THEN 1 END) as monthly
            FROM reviews
            WHERE product_id = ? AND approved = true AND deleted = false
        """;

        Map<String, Object> result = jdbcUtils.queryForMap(sql, productId);
        if (!result.isEmpty()) {
            velocity.put("reviewsPerDay", ((Number) result.get("daily")).longValue());
            velocity.put("reviewsPerWeek", ((Number) result.get("weekly")).longValue());
            velocity.put("reviewsPerMonth", ((Number) result.get("monthly")).longValue());
        }

        return velocity;
    }

    @Override
    public Map<String, Object> getVerificationStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN verified_purchase = true THEN 1 END) as verified,
                AVG(CASE WHEN verified_purchase = true THEN rating END) as verified_avg_rating,
                AVG(CASE WHEN verified_purchase = false THEN rating END) as unverified_avg_rating
            FROM reviews
            WHERE product_id = ? AND approved = true AND deleted = false
        """;

        Map<String, Object> result = jdbcUtils.queryForMap(sql, productId);
        if (!result.isEmpty()) {
            long total = ((Number) result.get("total")).longValue();
            long verified = ((Number) result.get("verified")).longValue();

            stats.put("totalReviews", total);
            stats.put("verifiedPurchases", verified);
            stats.put("unverifiedPurchases", total - verified);
            stats.put("verifiedPercentage", total > 0 ? (verified * 100.0) / total : 0.0);
            stats.put("verifiedAverageRating", result.get("verified_avg_rating") != null ?
                    ((Number) result.get("verified_avg_rating")).doubleValue() : 0.0);
            stats.put("unverifiedAverageRating", result.get("unverified_avg_rating") != null ?
                    ((Number) result.get("unverified_avg_rating")).doubleValue() : 0.0);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getHelpfulnessStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                AVG(helpful_count) as avg_helpful,
                AVG(not_helpful_count) as avg_not_helpful,
                SUM(helpful_count) as total_helpful,
                SUM(not_helpful_count) as total_not_helpful,
                COUNT(CASE WHEN helpful_count > not_helpful_count THEN 1 END) as positive_reviews
            FROM reviews
            WHERE product_id = ? AND approved = true AND deleted = false
        """;

        Map<String, Object> result = jdbcUtils.queryForMap(sql, productId);
        if (!result.isEmpty()) {
            stats.put("averageHelpfulVotes", result.get("avg_helpful") != null ?
                    ((Number) result.get("avg_helpful")).doubleValue() : 0.0);
            stats.put("averageNotHelpfulVotes", result.get("avg_not_helpful") != null ?
                    ((Number) result.get("avg_not_helpful")).doubleValue() : 0.0);
            stats.put("totalHelpfulVotes", ((Number) result.get("total_helpful")).longValue());
            stats.put("totalNotHelpfulVotes", ((Number) result.get("total_not_helpful")).longValue());
            stats.put("positiveReviewCount", ((Number) result.get("positive_reviews")).longValue());
        }

        return stats;
    }
}