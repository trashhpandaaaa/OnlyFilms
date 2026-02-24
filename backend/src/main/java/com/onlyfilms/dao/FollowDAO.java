package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Follow;
import com.onlyfilms.model.Profile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FollowDAO {

    public boolean follow(int followerId, int followingId) throws SQLException {
        if (followerId == followingId) return false;
        String sql = "INSERT IGNORE INTO follows_list (follower_id, following_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean unfollow(int followerId, int followingId) throws SQLException {
        String sql = "DELETE FROM follows_list WHERE follower_id = ? AND following_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean isFollowing(int followerId, int followingId) throws SQLException {
        String sql = "SELECT 1 FROM follows_list WHERE follower_id = ? AND following_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, followerId);
            stmt.setInt(2, followingId);
            return stmt.executeQuery().next();
        }
    }

    /** Get profiles that a user follows */
    public List<Profile> getFollowing(int profileId) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.user_id, p.display_name, p.bio, p.favorite_movie,
                   p.profile_pic, p.join_date_id, u.email,
                   d.full_date AS join_date
            FROM follows_list fl
            JOIN profiles p ON fl.following_id = p.profile_id
            JOIN users u ON p.user_id = u.user_id
            LEFT JOIN date_dim d ON p.join_date_id = d.date_id
            WHERE fl.follower_id = ?
            """;
        return executeProfileList(sql, profileId);
    }

    /** Get profiles that follow a user */
    public List<Profile> getFollowers(int profileId) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.user_id, p.display_name, p.bio, p.favorite_movie,
                   p.profile_pic, p.join_date_id, u.email,
                   d.full_date AS join_date
            FROM follows_list fl
            JOIN profiles p ON fl.follower_id = p.profile_id
            JOIN users u ON p.user_id = u.user_id
            LEFT JOIN date_dim d ON p.join_date_id = d.date_id
            WHERE fl.following_id = ?
            """;
        return executeProfileList(sql, profileId);
    }

    public int getFollowerCount(int profileId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM follows_list WHERE following_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int getFollowingCount(int profileId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM follows_list WHERE follower_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private List<Profile> executeProfileList(String sql, int profileId) throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Profile p = new Profile();
                p.setProfileId(rs.getInt("profile_id"));
                p.setUserId(rs.getInt("user_id"));
                p.setDisplayName(rs.getString("display_name"));
                p.setBio(rs.getString("bio"));
                p.setFavoriteMovie(rs.getString("favorite_movie"));
                p.setProfilePic(rs.getString("profile_pic"));
                p.setJoinDateId(rs.getInt("join_date_id"));
                p.setEmail(rs.getString("email"));
                Date joinDate = rs.getDate("join_date");
                if (joinDate != null) p.setJoinDate(joinDate.toString());
                profiles.add(p);
            }
        }
        return profiles;
    }
}
