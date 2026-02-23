package com.onlyfilms.service;

import com.onlyfilms.dao.MovieDAO;
import com.onlyfilms.dao.ReviewDAO;
import com.onlyfilms.model.Review;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for review operations with rating aggregation
 */
public class ReviewService {
    
    private final ReviewDAO reviewDAO;
    private final MovieDAO movieDAO;
    private final TmdbApiService tmdbService;

    public ReviewService() {
        this.reviewDAO = new ReviewDAO();
        this.movieDAO = new MovieDAO();
        this.tmdbService = TmdbApiService.getInstance();
    }

    /**
     * Create a new review
     */
    public ReviewResult createReview(int userId, int movieId, int rating, String content, boolean containsSpoilers) {
        // Validate rating
        if (rating < 1 || rating > 10) {
            return ReviewResult.error("Rating must be between 1 and 10");
        }

        // Check if user already reviewed this movie
        if (reviewDAO.existsByUserAndMovie(userId, movieId)) {
            return ReviewResult.error("You have already reviewed this movie");
        }

        // Ensure the movie exists in our database (fetch from TMDB if needed)
        try {
            Map<String, Object> tmdbMovie = tmdbService.getMovieDetails(movieId);
            String title = (String) tmdbMovie.get("title");
            String posterUrl = (String) tmdbMovie.get("posterUrl");
            movieDAO.ensureMovieExists(movieId, title, posterUrl);
        } catch (Exception e) {
            System.err.println("Warning: Could not fetch movie from TMDB: " + e.getMessage());
            // Still try to create a placeholder
            movieDAO.ensureMovieExists(movieId, "Unknown Movie", null);
        }

        // Create review
        Review review = new Review(userId, movieId, rating, content, containsSpoilers);
        
        try {
            review = reviewDAO.save(review);
            
            // Update movie's average rating
            updateMovieRating(movieId);
            
            return ReviewResult.success(review);
        } catch (SQLException e) {
            System.err.println("Error creating review: " + e.getMessage());
            return ReviewResult.error("Failed to create review");
        }
    }

    /**
     * Update an existing review
     */
    public ReviewResult updateReview(int reviewId, int userId, int rating, String content, boolean containsSpoilers) {
        // Validate rating
        if (rating < 1 || rating > 10) {
            return ReviewResult.error("Rating must be between 1 and 10");
        }

        // Find existing review
        Optional<Review> existingReview = reviewDAO.findById(reviewId);
        if (existingReview.isEmpty()) {
            return ReviewResult.error("Review not found");
        }

        Review review = existingReview.get();
        
        // Check ownership
        if (review.getUserId() != userId) {
            return ReviewResult.error("You can only edit your own reviews");
        }

        // Update review
        review.setRating(rating);
        review.setContent(content);
        review.setContainsSpoilers(containsSpoilers);

        if (reviewDAO.update(review)) {
            // Update movie's average rating
            updateMovieRating(review.getMovieId());
            return ReviewResult.success(review);
        } else {
            return ReviewResult.error("Failed to update review");
        }
    }

    /**
     * Delete a review
     */
    public ReviewResult deleteReview(int reviewId, int userId) {
        // Find existing review
        Optional<Review> existingReview = reviewDAO.findById(reviewId);
        if (existingReview.isEmpty()) {
            return ReviewResult.error("Review not found");
        }

        Review review = existingReview.get();
        int movieId = review.getMovieId();

        // Check ownership
        if (review.getUserId() != userId) {
            return ReviewResult.error("You can only delete your own reviews");
        }

        if (reviewDAO.delete(reviewId, userId)) {
            // Update movie's average rating
            updateMovieRating(movieId);
            return ReviewResult.success(null);
        } else {
            return ReviewResult.error("Failed to delete review");
        }
    }

    /**
     * Get reviews for a movie
     */
    public List<Review> getReviewsByMovie(int movieId, int page, int limit) {
        return reviewDAO.findByMovieId(movieId, page, limit);
    }

    /**
     * Get reviews by a user
     */
    public List<Review> getReviewsByUser(int userId, int page, int limit) {
        return reviewDAO.findByUserId(userId, page, limit);
    }

    /**
     * Get user's review for a specific movie
     */
    public Optional<Review> getUserReviewForMovie(int userId, int movieId) {
        return reviewDAO.findByUserAndMovie(userId, movieId);
    }

    /**
     * Like a review
     */
    public boolean likeReview(int userId, int reviewId) {
        return reviewDAO.likeReview(userId, reviewId);
    }

    /**
     * Unlike a review
     */
    public boolean unlikeReview(int userId, int reviewId) {
        return reviewDAO.unlikeReview(userId, reviewId);
    }

    /**
     * Check if user has liked a review
     */
    public boolean hasUserLiked(int userId, int reviewId) {
        return reviewDAO.hasUserLiked(userId, reviewId);
    }

    /**
     * Update movie's average rating after a review change
     */
    private void updateMovieRating(int movieId) {
        double avgRating = reviewDAO.getAverageRating(movieId);
        int ratingCount = reviewDAO.getRatingCount(movieId);
        movieDAO.updateRating(movieId, avgRating, ratingCount);
    }

    /**
     * Result class for review operations
     */
    public static class ReviewResult {
        private boolean success;
        private String message;
        private Review review;

        private ReviewResult(boolean success, String message, Review review) {
            this.success = success;
            this.message = message;
            this.review = review;
        }

        public static ReviewResult success(Review review) {
            return new ReviewResult(true, "Success", review);
        }

        public static ReviewResult error(String message) {
            return new ReviewResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Review getReview() {
            return review;
        }
    }
}
