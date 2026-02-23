package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for Review operations
 */
public class ReviewDAO {

    /**
     * Find review by ID
     */
    public Optional<Review> findById(int id) {
        String sql = """
            SELECT r.*, u.username, u.avatar_url as user_avatar_url, m.title as movie_title
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            WHERE r.id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding review by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find reviews by movie ID
     */
    public List<Review> findByMovieId(int movieId, int page, int limit) {
        String sql = """
            SELECT r.*, u.username, u.avatar_url as user_avatar_url, m.title as movie_title
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            WHERE r.movie_id = ?
            ORDER BY r.created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, movieId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding reviews by movie: " + e.getMessage());
        }
        return reviews;
    }

    /**
     * Find reviews by user ID
     */
    public List<Review> findByUserId(int userId, int page, int limit) {
        String sql = """
            SELECT r.*, u.username, u.avatar_url as user_avatar_url, m.title as movie_title
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            WHERE r.user_id = ?
            ORDER BY r.created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding reviews by user: " + e.getMessage());
        }
        return reviews;
    }

    /**
     * Find user's review for a specific movie
     */
    public Optional<Review> findByUserAndMovie(int userId, int movieId) {
        String sql = """
            SELECT r.*, u.username, u.avatar_url as user_avatar_url, m.title as movie_title
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            WHERE r.user_id = ? AND r.movie_id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding review by user and movie: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Get recent reviews (for activity feed)
     */
    public List<Review> findRecent(int limit) {
        String sql = """
            SELECT r.*, u.username, u.avatar_url as user_avatar_url, m.title as movie_title
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            JOIN movies m ON r.movie_id = m.id
            ORDER BY r.created_at DESC
            LIMIT ?
            """;
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding recent reviews: " + e.getMessage());
        }
        return reviews;
    }

    /**
     * Save a new review
     */
    public Review save(Review review) throws SQLException {
        String sql = """
            INSERT INTO reviews (user_id, movie_id, rating, content, contains_spoilers)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, review.getUserId());
            stmt.setInt(2, review.getMovieId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getContent());
            stmt.setBoolean(5, review.isContainsSpoilers());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating review failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    review.setId(generatedKeys.getInt(1));
                }
            }
        }
        return review;
    }

    /**
     * Update review
     */
    public boolean update(Review review) {
        String sql = "UPDATE reviews SET rating = ?, content = ?, contains_spoilers = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, review.getRating());
            stmt.setString(2, review.getContent());
            stmt.setBoolean(3, review.isContainsSpoilers());
            stmt.setInt(4, review.getId());
            stmt.setInt(5, review.getUserId()); // Ensure user owns the review
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating review: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete review
     */
    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM reviews WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId); // Ensure user owns the review
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting review: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has already reviewed a movie
     */
    public boolean existsByUserAndMovie(int userId, int movieId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking review existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get average rating for a movie
     */
    public double getAverageRating(int movieId) {
        String sql = "SELECT AVG(rating) as avg_rating FROM reviews WHERE movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (SQLException e) {
            System.err.println("Error getting average rating: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get rating count for a movie
     */
    public int getRatingCount(int movieId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting rating count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get rating distribution for a movie (how many 1-star, 2-star, etc.)
     * Ratings are stored as 1-10, we group them into 1-5 stars
     */
    public Map<Integer, Integer> getRatingDistribution(int movieId) {
        Map<Integer, Integer> distribution = new HashMap<>();
        // Initialize all star counts to 0
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        
        String sql = "SELECT CEIL(rating / 2) as stars, COUNT(*) as count FROM reviews WHERE movie_id = ? GROUP BY stars ORDER BY stars";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int stars = rs.getInt("stars");
                int count = rs.getInt("count");
                if (stars >= 1 && stars <= 5) {
                    distribution.put(stars, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting rating distribution: " + e.getMessage());
        }
        return distribution;
    }

    /**
     * Like a review
     */
    public boolean likeReview(int userId, int reviewId) {
        String sql = "INSERT IGNORE INTO review_likes (user_id, review_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, reviewId);
            
            if (stmt.executeUpdate() > 0) {
                // Update like count
                updateLikeCount(reviewId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error liking review: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unlike a review
     */
    public boolean unlikeReview(int userId, int reviewId) {
        String sql = "DELETE FROM review_likes WHERE user_id = ? AND review_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, reviewId);
            
            if (stmt.executeUpdate() > 0) {
                // Update like count
                updateLikeCount(reviewId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error unliking review: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if user has liked a review
     */
    public boolean hasUserLiked(int userId, int reviewId) {
        String sql = "SELECT COUNT(*) FROM review_likes WHERE user_id = ? AND review_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, reviewId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking like status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update like count for a review
     */
    private void updateLikeCount(int reviewId) {
        String sql = "UPDATE reviews SET like_count = (SELECT COUNT(*) FROM review_likes WHERE review_id = ?) WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reviewId);
            stmt.setInt(2, reviewId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating like count: " + e.getMessage());
        }
    }

    /**
     * Count reviews for a movie
     */
    public int countByMovieId(int movieId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting reviews: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Count reviews by user ID (for profile stats)
     */
    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting user reviews: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Map ResultSet row to Review object
     */
    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review review = new Review(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getInt("movie_id"),
            rs.getInt("rating"),
            rs.getString("content"),
            rs.getBoolean("contains_spoilers"),
            rs.getInt("likes_count"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
        
        // Set additional display fields if available
        try {
            review.setUsername(rs.getString("username"));
            review.setUserAvatarUrl(rs.getString("user_avatar_url"));
            review.setMovieTitle(rs.getString("movie_title"));
        } catch (SQLException e) {
            // Fields not in result set, ignore
        }
        
        return review;
    }
}
