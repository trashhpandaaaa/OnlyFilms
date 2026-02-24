package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Like;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LikeDAO {

    public boolean toggle(int activityId, int profileId) throws SQLException {
        // Check if like exists
        String checkSql = "SELECT like_id FROM `like` WHERE activity_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, activityId);
            stmt.setInt(2, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Unlike
                String deleteSql = "DELETE FROM `like` WHERE like_id = ?";
                try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                    del.setInt(1, rs.getInt("like_id"));
                    del.executeUpdate();
                }
                return false; // unliked
            } else {
                // Like
                String insertSql = "INSERT INTO `like` (activity_id, profile_id) VALUES (?, ?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setInt(1, activityId);
                    ins.setInt(2, profileId);
                    ins.executeUpdate();
                }
                return true; // liked
            }
        }
    }

    public int countByActivityId(int activityId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `like` WHERE activity_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public boolean isLikedBy(int activityId, int profileId) throws SQLException {
        String sql = "SELECT 1 FROM `like` WHERE activity_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            stmt.setInt(2, profileId);
            return stmt.executeQuery().next();
        }
    }

    public List<Like> findByActivityId(int activityId) throws SQLException {
        String sql = """
            SELECT l.like_id, l.activity_id, l.profile_id, p.display_name, p.profile_pic
            FROM `like` l
            JOIN profiles p ON l.profile_id = p.profile_id
            WHERE l.activity_id = ?
            """;
        List<Like> likes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Like like = new Like();
                like.setLikeId(rs.getInt("like_id"));
                like.setActivityId(rs.getInt("activity_id"));
                like.setProfileId(rs.getInt("profile_id"));
                like.setDisplayName(rs.getString("display_name"));
                like.setProfilePic(rs.getString("profile_pic"));
                likes.add(like);
            }
        }
        return likes;
    }
}
