package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.common.utils.JdbcUtils;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ProductRatingStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository.ReviewRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC-based Review Repository Implementation
 */
@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

    private final JdbcUtils jdbcUtils;

    public ReviewRepositoryImpl(JdbcUtils jdbcUtils) {
        this.jdbcUtils = jdbcUtils;
    }

    /**
     * RowMapper for Review entity
     */
    private static class ReviewRowMapper implements RowMapper<Review> {
        @Override
        public Review mapRow(ResultSet rs, int rowNum) throws SQLException {
            Review review = new Review();
            review.setId(rs.getLong("id"));
            review.setRating(rs.getInt("rating"));
            review.setTitle(rs.getString("title"));
            review.setComment(rs.getString("comment"));
            review.setVerifiedPurchase(rs.getBoolean("verified_purchase"));
            review.setApproved(rs.getBoolean("approved"));
            review.setHelpfulCount(rs.getInt("helpful_count"));
            review.setNotHelpfulCount(rs.getInt("not_helpful_count"));
            review.setAdminResponse(rs.getString("admin_response"));
            review.setIsActive(rs.getBoolean("is_active"));

            if (rs.getTimestamp("created_at") != null) {
                review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                review.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return review;
        }
    }

    @Override
    public Review save(Review review) {
        String sql = """
            INSERT INTO reviews (
                product_id, user_id, rating, title, comment, 
                verified_purchase, approved, helpful_count, not_helpful_count,
                admin_response, is_active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql,
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getUser() != null ? review.getUser().getId() : null,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getVerifiedPurchase() != null ? review.getVerifiedPurchase() : false,
                review.getApproved() != null ? review.getApproved() : false,
                review.getHelpfulCount() != null ? review.getHelpfulCount() : 0,
                review.getNotHelpfulCount() != null ? review.getNotHelpfulCount() : 0,
                review.getAdminResponse(),
                review.getIsActive() != null ? review.getIsActive() : true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        if (result.getGeneratedKey() != null) {
            review.setId(result.getGeneratedKey());
        }

        // Save images if any
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            saveReviewImages(review.getId(), review.getImages());
        }

        // Save pros if any
        if (review.getPros() != null && !review.getPros().isEmpty()) {
            saveReviewPros(review.getId(), review.getPros());
        }

        // Save cons if any
        if (review.getCons() != null && !review.getCons().isEmpty()) {
            saveReviewCons(review.getId(), review.getCons());
        }

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = """
            UPDATE reviews SET
                rating = ?, title = ?, comment = ?, verified_purchase = ?, 
                approved = ?, helpful_count = ?, not_helpful_count = ?,
                admin_response = ?, is_active = ?, updated_at = ?
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
                review.getIsActive(),
                LocalDateTime.now(),
                review.getId()
        );

        // Update images
        deleteReviewImages(review.getId());
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            saveReviewImages(review.getId(), review.getImages());
        }

        // Update pros
        deleteReviewPros(review.getId());
        if (review.getPros() != null && !review.getPros().isEmpty()) {
            saveReviewPros(review.getId(), review.getPros());
        }

        // Update cons
        deleteReviewCons(review.getId());
        if (review.getCons() != null && !review.getCons().isEmpty()) {
            saveReviewCons(review.getId(), review.getCons());
        }

        return review;
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), id);

        if (reviews.isEmpty()) {
            return Optional.empty();
        }

        Review review = reviews.get(0);
        loadReviewCollections(review);
        return Optional.of(review);
    }

    @Override
    public List<Review> findAll() {
        String sql = "SELECT * FROM reviews ORDER BY created_at DESC";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper());
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findAll(int page, int size) {
        String sql = "SELECT * FROM reviews ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findByProductId(Long productId, int page, int size) {
        String sql = "SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findByProductIdAndApprovedTrue(Long productId, int page, int size) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND approved = true 
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findByUserId(Long userId, int page, int size) {
        String sql = "SELECT * FROM reviews WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), userId, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findPendingReviews(int page, int size) {
        String sql = "SELECT * FROM reviews WHERE approved = false ORDER BY created_at ASC LIMIT ? OFFSET ?";
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findVerifiedPurchaseReviews(Long productId, int page, int size) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND verified_purchase = true AND approved = true
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findMostHelpfulReviews(Long productId, int limit) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND approved = true
            ORDER BY helpful_count DESC, created_at DESC
            LIMIT ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, limit);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findByProductIdAndRating(Long productId, Integer rating, int page, int size) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND rating = ? AND approved = true
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, rating, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findRecentReviews(Long productId, int limit) {
        String sql = """
            SELECT * FROM reviews 
            WHERE product_id = ? AND approved = true
            ORDER BY created_at DESC 
            LIMIT ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, limit);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public List<Review> findReviewsWithImages(Long productId, int page, int size) {
        String sql = """
            SELECT DISTINCT r.* FROM reviews r
            INNER JOIN review_images ri ON r.id = ri.review_id
            WHERE r.product_id = ? AND r.approved = true
            ORDER BY r.created_at DESC 
            LIMIT ? OFFSET ?
        """;
        List<Review> reviews = jdbcUtils.query(sql, new ReviewRowMapper(), productId, size, page * size);
        reviews.forEach(this::loadReviewCollections);
        return reviews;
    }

    @Override
    public boolean existsByProductIdAndUserId(Long productId, Long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND user_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId, userId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM reviews";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countByProductId(Long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ?";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId);
        return count != null ? count : 0L;
    }

    @Override
    public long countByProductIdAndApprovedTrue(Long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND approved = true";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId);
        return count != null ? count : 0L;
    }

    @Override
    public long countPendingReviews() {
        String sql = "SELECT COUNT(*) FROM reviews WHERE approved = false";
        Long count = jdbcUtils.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countVerifiedPurchaseReviews(Long productId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ? AND verified_purchase = true";
        Long count = jdbcUtils.queryForObject(sql, Long.class, productId);
        return count != null ? count : 0L;
    }

    @Override
    public Double getAverageRatingByProductId(Long productId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE product_id = ? AND approved = true";
        Double avg = jdbcUtils.queryForObject(sql, Double.class, productId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public Map<Integer, Long> getRatingDistribution(Long productId) {
        String sql = """
            SELECT rating, COUNT(*) as count 
            FROM reviews 
            WHERE product_id = ? AND approved = true
            GROUP BY rating 
            ORDER BY rating DESC
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, productId);
        Map<Integer, Long> distribution = new HashMap<>();

        // Initialize all ratings with 0
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        // Fill in actual counts
        for (Map<String, Object> row : result.getResultSet()) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            distribution.put(rating, count);
        }

        return distribution;
    }

    @Override
    public ProductRatingStats getProductRatingStats(Long productId) {
        Double avgRating = getAverageRatingByProductId(productId);
        Long totalReviews = countByProductIdAndApprovedTrue(productId);
        Map<Integer, Long> distribution = getRatingDistribution(productId);

        ProductRatingStats.RatingDistribution ratingDist = ProductRatingStats.RatingDistribution.builder()
                .fiveStars(distribution.getOrDefault(5, 0L))
                .fourStars(distribution.getOrDefault(4, 0L))
                .threeStars(distribution.getOrDefault(3, 0L))
                .twoStars(distribution.getOrDefault(2, 0L))
                .oneStar(distribution.getOrDefault(1, 0L))
                .build();

        return ProductRatingStats.builder()
                .productId(productId)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .distribution(ratingDist)
                .build();
    }

    @Override
    public boolean approveReview(Long reviewId) {
        String sql = "UPDATE reviews SET approved = true, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, LocalDateTime.now(), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean rejectReview(Long reviewId) {
        String sql = "UPDATE reviews SET approved = false, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, LocalDateTime.now(), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean incrementHelpfulCount(Long reviewId) {
        String sql = "UPDATE reviews SET helpful_count = helpful_count + 1, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, LocalDateTime.now(), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean incrementNotHelpfulCount(Long reviewId) {
        String sql = "UPDATE reviews SET not_helpful_count = not_helpful_count + 1, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, LocalDateTime.now(), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean addAdminResponse(Long reviewId, String response) {
        String sql = "UPDATE reviews SET admin_response = ?, updated_at = ? WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, response, LocalDateTime.now(), reviewId);
        return result.getAffectedRows() > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        // Delete related data first
        deleteReviewImages(id);
        deleteReviewPros(id);
        deleteReviewCons(id);

        String sql = "DELETE FROM reviews WHERE id = ?";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, id);
        return result.getAffectedRows() > 0;
    }

    @Override
    public int deleteByProductId(Long productId) {
        // Get all review IDs for this product
        String selectSql = "SELECT id FROM reviews WHERE product_id = ?";
        JdbcUtils.QueryResult selectResult = jdbcUtils.executePreparedQuery(selectSql, productId);

        int deletedCount = 0;
        for (Map<String, Object> row : selectResult.getResultSet()) {
            Long reviewId = ((Number) row.get("id")).longValue();
            if (deleteById(reviewId)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    @Override
    public int deleteByUserId(Long userId) {
        // Get all review IDs for this user
        String selectSql = "SELECT id FROM reviews WHERE user_id = ?";
        JdbcUtils.QueryResult selectResult = jdbcUtils.executePreparedQuery(selectSql, userId);

        int deletedCount = 0;
        for (Map<String, Object> row : selectResult.getResultSet()) {
            Long reviewId = ((Number) row.get("id")).longValue();
            if (deleteById(reviewId)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    @Override
    public List<Map<String, Object>> getTopRatedProducts(int limit) {
        String sql = """
            SELECT 
                r.product_id,
                AVG(r.rating) as average_rating,
                COUNT(r.id) as review_count
            FROM reviews r
            WHERE r.approved = true
            GROUP BY r.product_id
            HAVING COUNT(r.id) >= 5
            ORDER BY average_rating DESC, review_count DESC
            LIMIT ?
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, limit);
        return result.getResultSet();
    }

    @Override
    public List<Map<String, Object>> getMostReviewedProducts(int limit) {
        String sql = """
            SELECT 
                r.product_id,
                COUNT(r.id) as review_count,
                AVG(r.rating) as average_rating
            FROM reviews r
            WHERE r.approved = true
            GROUP BY r.product_id
            ORDER BY review_count DESC, average_rating DESC
            LIMIT ?
        """;

        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, limit);
        return result.getResultSet();
    }

    @Override
    public Map<String, Object> getUserReviewStats(Long userId) {
        String sql = """
            SELECT 
                COUNT(*) as total_reviews,
                AVG(rating) as average_rating_given,
                SUM(CASE WHEN verified_purchase = true THEN 1 ELSE 0 END) as verified_reviews,
                SUM(CASE WHEN approved = true THEN 1 ELSE 0 END) as approved_reviews,
                SUM(helpful_count) as total_helpful_votes,
                MAX(created_at) as last_review_date
            FROM reviews
            WHERE user_id = ?
        """;

        Map<String, Object> stats = jdbcUtils.queryForMap(sql, userId);
        return stats != null ? stats : new HashMap<>();
    }

    // ========================================================================
    // HELPER METHODS FOR COLLECTIONS
    // ========================================================================

    /**
     * Load all collections for a review (images, pros, cons)
     */
    private void loadReviewCollections(Review review) {
        review.setImages(loadReviewImages(review.getId()));
        review.setPros(loadReviewPros(review.getId()));
        review.setCons(loadReviewCons(review.getId()));
    }

    /**
     * Load review images
     */
    private List<String> loadReviewImages(Long reviewId) {
        String sql = "SELECT image_url FROM review_images WHERE review_id = ? ORDER BY id";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, reviewId);

        return result.getResultSet().stream()
                .map(row -> (String) row.get("image_url"))
                .collect(Collectors.toList());
    }

    /**
     * Load review pros
     */
    private List<String> loadReviewPros(Long reviewId) {
        String sql = "SELECT pro FROM review_pros WHERE review_id = ? ORDER BY id";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, reviewId);

        return result.getResultSet().stream()
                .map(row -> (String) row.get("pro"))
                .collect(Collectors.toList());
    }

    /**
     * Load review cons
     */
    private List<String> loadReviewCons(Long reviewId) {
        String sql = "SELECT con FROM review_cons WHERE review_id = ? ORDER BY id";
        JdbcUtils.QueryResult result = jdbcUtils.executePreparedQuery(sql, reviewId);

        return result.getResultSet().stream()
                .map(row -> (String) row.get("con"))
                .collect(Collectors.toList());
    }

    /**
     * Save review images
     */
    private void saveReviewImages(Long reviewId, List<String> images) {
        String sql = "INSERT INTO review_images (review_id, image_url) VALUES (?, ?)";
        for (String imageUrl : images) {
            jdbcUtils.executePreparedQuery(sql, reviewId, imageUrl);
        }
    }

    /**
     * Save review pros
     */
    private void saveReviewPros(Long reviewId, List<String> pros) {
        String sql = "INSERT INTO review_pros (review_id, pro) VALUES (?, ?)";
        for (String pro : pros) {
            jdbcUtils.executePreparedQuery(sql, reviewId, pro);
        }
    }

    /**
     * Save review cons
     */
    private void saveReviewCons(Long reviewId, List<String> cons) {
        String sql = "INSERT INTO review_cons (review_id, con) VALUES (?, ?)";
        for (String con : cons) {
            jdbcUtils.executePreparedQuery(sql, reviewId, con);
        }
    }

    /**
     * Delete review images
     */
    private void deleteReviewImages(Long reviewId) {
        String sql = "DELETE FROM review_images WHERE review_id = ?";
        jdbcUtils.executePreparedQuery(sql, reviewId);
    }

    /**
     * Delete review pros
     */
    private void deleteReviewPros(Long reviewId) {
        String sql = "DELETE FROM review_pros WHERE review_id = ?";
        jdbcUtils.executePreparedQuery(sql, reviewId);
    }

    /**
     * Delete review cons
     */
    private void deleteReviewCons(Long reviewId) {
        String sql = "DELETE FROM review_cons WHERE review_id = ?";
        jdbcUtils.executePreparedQuery(sql, reviewId);
    }
}