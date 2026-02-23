package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Activity;
import com.onlyfilms.model.Activity.ActivityType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Activity Feed operations
 */
public class ActivityDAO {

    /**
     * Get activity feed for a user (from people they follow)
     */
    public List<Activity> getFeed(int userId, int page, int limit) {
        // Get combined activities from followed users
        String sql = """
            (
                -- Reviews from followed users
                SELECT 'REVIEW' as type, r.created_at as timestamp,
                       r.user_id, u.username, u.avatar_url as user_avatar,
                       r.movie_id, m.title as movie_title, m.poster_url as movie_poster,
                       r.id as review_id, r.rating, 
                       SUBSTRING(r.content, 1, 200) as review_content,
                       NULL as list_id, NULL as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM reviews r
                JOIN users u ON r.user_id = u.id
                JOIN movies m ON r.movie_id = m.id
                WHERE r.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?)
            )
            UNION ALL
            (
                -- Watch history from followed users
                SELECT 'WATCH' as type, wh.created_at as timestamp,
                       wh.user_id, u.username, u.avatar_url as user_avatar,
                       wh.movie_id, m.title as movie_title, m.poster_url as movie_poster,
                       NULL as review_id, NULL as rating, NULL as review_content,
                       NULL as list_id, NULL as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM watch_history wh
                JOIN users u ON wh.user_id = u.id
                JOIN movies m ON wh.movie_id = m.id
                WHERE wh.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?)
            )
            UNION ALL
            (
                -- Lists created by followed users
                SELECT 'LIST_CREATE' as type, cl.created_at as timestamp,
                       cl.user_id, u.username, u.avatar_url as user_avatar,
                       NULL as movie_id, NULL as movie_title, NULL as movie_poster,
                       NULL as review_id, NULL as rating, NULL as review_content,
                       cl.id as list_id, cl.name as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM custom_lists cl
                JOIN users u ON cl.user_id = u.id
                WHERE cl.is_public = TRUE
                  AND cl.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?)
            )
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """;
        
        List<Activity> activities = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, limit);
            stmt.setInt(5, (page - 1) * limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapResultSetToActivity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting feed: " + e.getMessage());
        }
        return activities;
    }

    /**
     * Get activity for a specific user (their profile activity)
     */
    public List<Activity> getUserActivity(int userId, int page, int limit) {
        String sql = """
            (
                -- User's reviews
                SELECT 'REVIEW' as type, r.created_at as timestamp,
                       r.user_id, u.username, u.avatar_url as user_avatar,
                       r.movie_id, m.title as movie_title, m.poster_url as movie_poster,
                       r.id as review_id, r.rating,
                       SUBSTRING(r.content, 1, 200) as review_content,
                       NULL as list_id, NULL as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM reviews r
                JOIN users u ON r.user_id = u.id
                JOIN movies m ON r.movie_id = m.id
                WHERE r.user_id = ?
            )
            UNION ALL
            (
                -- User's watch history
                SELECT 'WATCH' as type, wh.created_at as timestamp,
                       wh.user_id, u.username, u.avatar_url as user_avatar,
                       wh.movie_id, m.title as movie_title, m.poster_url as movie_poster,
                       NULL as review_id, NULL as rating, NULL as review_content,
                       NULL as list_id, NULL as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM watch_history wh
                JOIN users u ON wh.user_id = u.id
                JOIN movies m ON wh.movie_id = m.id
                WHERE wh.user_id = ?
            )
            UNION ALL
            (
                -- User's public lists
                SELECT 'LIST_CREATE' as type, cl.created_at as timestamp,
                       cl.user_id, u.username, u.avatar_url as user_avatar,
                       NULL as movie_id, NULL as movie_title, NULL as movie_poster,
                       NULL as review_id, NULL as rating, NULL as review_content,
                       cl.id as list_id, cl.name as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM custom_lists cl
                JOIN users u ON cl.user_id = u.id
                WHERE cl.is_public = TRUE AND cl.user_id = ?
            )
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """;
        
        List<Activity> activities = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, limit);
            stmt.setInt(5, (page - 1) * limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapResultSetToActivity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting user activity: " + e.getMessage());
        }
        return activities;
    }

    /**
     * Get global/recent activity (for discovery)
     */
    public List<Activity> getRecentActivity(int page, int limit) {
        String sql = """
            (
                -- Recent reviews
                SELECT 'REVIEW' as type, r.created_at as timestamp,
                       r.user_id, u.username, u.avatar_url as user_avatar,
                       r.movie_id, m.title as movie_title, m.poster_url as movie_poster,
                       r.id as review_id, r.rating,
                       SUBSTRING(r.content, 1, 200) as review_content,
                       NULL as list_id, NULL as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM reviews r
                JOIN users u ON r.user_id = u.id
                JOIN movies m ON r.movie_id = m.id
            )
            UNION ALL
            (
                -- Recent public lists
                SELECT 'LIST_CREATE' as type, cl.created_at as timestamp,
                       cl.user_id, u.username, u.avatar_url as user_avatar,
                       NULL as movie_id, NULL as movie_title, NULL as movie_poster,
                       NULL as review_id, NULL as rating, NULL as review_content,
                       cl.id as list_id, cl.name as list_name,
                       NULL as target_user_id, NULL as target_username
                FROM custom_lists cl
                JOIN users u ON cl.user_id = u.id
                WHERE cl.is_public = TRUE
            )
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """;
        
        List<Activity> activities = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, (page - 1) * limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapResultSetToActivity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent activity: " + e.getMessage());
        }
        return activities;
    }

    private Activity mapResultSetToActivity(ResultSet rs) throws SQLException {
        Activity activity = new Activity();
        
        String type = rs.getString("type");
        activity.setType(ActivityType.valueOf(type));
        activity.setTimestamp(rs.getTimestamp("timestamp"));
        
        activity.setUserId(rs.getInt("user_id"));
        activity.setUsername(rs.getString("username"));
        activity.setUserAvatarUrl(rs.getString("user_avatar"));
        
        // Movie info
        int movieId = rs.getInt("movie_id");
        if (!rs.wasNull()) {
            activity.setMovieId(movieId);
            activity.setMovieTitle(rs.getString("movie_title"));
            activity.setMoviePosterUrl(rs.getString("movie_poster"));
        }
        
        // Review info
        int reviewId = rs.getInt("review_id");
        if (!rs.wasNull()) {
            activity.setReviewId(reviewId);
            activity.setRating(rs.getInt("rating"));
            activity.setReviewContent(rs.getString("review_content"));
        }
        
        // List info
        int listId = rs.getInt("list_id");
        if (!rs.wasNull()) {
            activity.setListId(listId);
            activity.setListName(rs.getString("list_name"));
        }
        
        // Target user info
        int targetUserId = rs.getInt("target_user_id");
        if (!rs.wasNull()) {
            activity.setTargetUserId(targetUserId);
            activity.setTargetUsername(rs.getString("target_username"));
        }
        
        return activity;
    }
}
