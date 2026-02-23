package com.onlyfilms.service;

import com.onlyfilms.dao.ReviewDAO;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating combined movie ratings from multiple sources.
 * Combines TMDB (critic/community) ratings with OnlyFilms user ratings.
 */
public class RatingService {
    
    private final ReviewDAO reviewDAO;
    
    // Weight factors for combined rating (can be adjusted)
    private static final double TMDB_WEIGHT = 0.6;  // 60% weight for TMDB/critics
    private static final double USER_WEIGHT = 0.4;  // 40% weight for user ratings
    
    public RatingService() {
        this.reviewDAO = new ReviewDAO();
    }
    
    /**
     * Get comprehensive rating data for a movie
     * @param movieId The movie ID
     * @param tmdbRating The TMDB rating (0-5 scale)
     * @param tmdbRatingCount The number of TMDB votes
     * @return Map containing all rating information
     */
    public Map<String, Object> getMovieRatings(int movieId, Double tmdbRating, Integer tmdbRatingCount) {
        Map<String, Object> ratings = new HashMap<>();
        
        // TMDB/Critics rating (already on 0-5 scale)
        double criticRating = tmdbRating != null ? tmdbRating : 0.0;
        int criticCount = tmdbRatingCount != null ? tmdbRatingCount : 0;
        
        // User ratings from our database (stored as 1-10, convert to 0-5)
        double userRatingRaw = reviewDAO.getAverageRating(movieId);
        double userRating = userRatingRaw > 0 ? userRatingRaw / 2.0 : 0.0;
        int userCount = reviewDAO.getRatingCount(movieId);
        
        // Calculate combined rating
        double combinedRating = calculateCombinedRating(criticRating, criticCount, userRating, userCount);
        
        // Critic score (TMDB) - out of 5 stars
        Map<String, Object> critic = new HashMap<>();
        critic.put("rating", Math.round(criticRating * 100.0) / 100.0);
        critic.put("count", criticCount);
        critic.put("percentage", Math.round(criticRating * 20.0)); // Convert to percentage (0-100)
        critic.put("source", "TMDB");
        ratings.put("critic", critic);
        
        // User score (OnlyFilms users) - out of 5 stars
        Map<String, Object> user = new HashMap<>();
        user.put("rating", Math.round(userRating * 100.0) / 100.0);
        user.put("count", userCount);
        user.put("percentage", Math.round(userRating * 20.0)); // Convert to percentage (0-100)
        user.put("source", "OnlyFilms Users");
        ratings.put("user", user);
        
        // Combined/Overall rating
        Map<String, Object> combined = new HashMap<>();
        combined.put("rating", Math.round(combinedRating * 100.0) / 100.0);
        combined.put("totalCount", criticCount + userCount);
        combined.put("percentage", Math.round(combinedRating * 20.0));
        ratings.put("combined", combined);
        
        // Rating breakdown for display
        ratings.put("hasCriticRating", criticCount > 0);
        ratings.put("hasUserRating", userCount > 0);
        ratings.put("primaryRating", combinedRating > 0 ? combinedRating : criticRating);
        
        return ratings;
    }
    
    /**
     * Calculate a weighted combined rating from critic and user scores.
     * Uses a weighted average that considers the number of votes from each source.
     */
    private double calculateCombinedRating(double criticRating, int criticCount, double userRating, int userCount) {
        // If neither has ratings, return 0
        if (criticCount == 0 && userCount == 0) {
            return 0.0;
        }
        
        // If only critics have rated
        if (userCount == 0) {
            return criticRating;
        }
        
        // If only users have rated
        if (criticCount == 0) {
            return userRating;
        }
        
        // Both have ratings - use weighted average
        // The more votes a source has, the more reliable it is
        // We use a base weight plus a vote-count factor
        
        double criticVoteWeight = Math.min(1.0, criticCount / 1000.0); // Caps at 1000 votes
        double userVoteWeight = Math.min(1.0, userCount / 50.0);       // Caps at 50 votes (smaller community)
        
        double effectiveCriticWeight = TMDB_WEIGHT * criticVoteWeight;
        double effectiveUserWeight = USER_WEIGHT * userVoteWeight;
        
        // Normalize weights
        double totalWeight = effectiveCriticWeight + effectiveUserWeight;
        if (totalWeight == 0) {
            return (criticRating + userRating) / 2.0; // Simple average fallback
        }
        
        return (criticRating * effectiveCriticWeight + userRating * effectiveUserWeight) / totalWeight;
    }
    
    /**
     * Get just the user rating stats for a movie
     */
    public Map<String, Object> getUserRatingStats(int movieId) {
        Map<String, Object> stats = new HashMap<>();
        
        double avgRating = reviewDAO.getAverageRating(movieId);
        int ratingCount = reviewDAO.getRatingCount(movieId);
        
        // Convert from 1-10 scale to 0-5 scale
        double rating = avgRating > 0 ? avgRating / 2.0 : 0.0;
        
        stats.put("rating", Math.round(rating * 100.0) / 100.0);
        stats.put("count", ratingCount);
        stats.put("percentage", Math.round(rating * 20.0));
        
        // Rating distribution (1-5 stars)
        Map<Integer, Integer> distribution = reviewDAO.getRatingDistribution(movieId);
        stats.put("distribution", distribution);
        
        return stats;
    }
}
