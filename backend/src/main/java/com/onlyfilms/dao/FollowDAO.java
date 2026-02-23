package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Follow;
import com.onlyfilms.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Follow operations
 */
public class FollowDAO {

    /**
     * Follow a user
     */
    public boolean follow(int followerId, int followingId) {
        // Can't follow yourself
        if (followerId == followingId) {
            return false;
        }

        String sql = "INSERT IGNORE INTO follows (follower_id, following_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error following user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unfollow a user
     */
    public boolean unfollow(int followerId, int followingId) {
        String sql = "DELETE FROM follows WHERE follower_id = ? AND following_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error unfollowing user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if user is following another user
     */
    public boolean isFollowing(int followerId, int followingId) {
        String sql = "SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking follow status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get followers of a user
     */
    public List<User> getFollowers(int userId, int page, int limit) {
        String sql = """
            SELECT u.id, u.username, u.bio, u.avatar_url, u.created_at
            FROM follows f
            JOIN users u ON f.follower_id = u.id
            WHERE f.following_id = ?
            ORDER BY f.created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<User> followers = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                followers.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting followers: " + e.getMessage());
        }
        return followers;
    }

    /**
     * Get users that a user is following
     */
    public List<User> getFollowing(int userId, int page, int limit) {
        String sql = """
            SELECT u.id, u.username, u.bio, u.avatar_url, u.created_at
            FROM follows f
            JOIN users u ON f.following_id = u.id
            WHERE f.follower_id = ?
            ORDER BY f.created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<User> following = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                following.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting following: " + e.getMessage());
        }
        return following;
    }

    /**
     * Count followers
     */
    public int countFollowers(int userId) {
        String sql = "SELECT COUNT(*) FROM follows WHERE following_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting followers: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Count following
     */
    public int countFollowing(int userId) {
        String sql = "SELECT COUNT(*) FROM follows WHERE follower_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting following: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get IDs of users that a user is following (for activity feed)
     */
    public List<Integer> getFollowingIds(int userId) {
        String sql = "SELECT following_id FROM follows WHERE follower_id = ?";
        List<Integer> ids = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ids.add(rs.getInt("following_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting following IDs: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Search users by username
     */
    public List<User> searchUsers(String query, int page, int limit) {
        String sql = """
            SELECT id, username, bio, avatar_url, created_at
            FROM users
            WHERE username LIKE ?
            ORDER BY username
            LIMIT ? OFFSET ?
            """;
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + query + "%");
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setBio(rs.getString("bio"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        return user;
    }
}
