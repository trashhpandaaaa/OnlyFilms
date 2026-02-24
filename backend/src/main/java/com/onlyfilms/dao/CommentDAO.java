package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    public Comment findById(int commentId) throws SQLException {
        String sql = """
            SELECT c.comment_id, c.activity_id, c.profile_id, c.comment_content, c.created_id,
                   p.display_name, p.profile_pic, d.full_date AS created_date
            FROM comment c
            JOIN profiles p ON c.profile_id = p.profile_id
            LEFT JOIN date_dim d ON c.created_id = d.date_id
            WHERE c.comment_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public List<Comment> findByActivityId(int activityId) throws SQLException {
        String sql = """
            SELECT c.comment_id, c.activity_id, c.profile_id, c.comment_content, c.created_id,
                   p.display_name, p.profile_pic, d.full_date AS created_date
            FROM comment c
            JOIN profiles p ON c.profile_id = p.profile_id
            LEFT JOIN date_dim d ON c.created_id = d.date_id
            WHERE c.activity_id = ?
            ORDER BY c.comment_id ASC
            """;
        List<Comment> comments = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                comments.add(mapRow(rs));
            }
        }
        return comments;
    }

    public Comment create(Comment comment) throws SQLException {
        String sql = "INSERT INTO comment (activity_id, profile_id, comment_content, created_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getActivityId());
            stmt.setInt(2, comment.getProfileId());
            stmt.setString(3, comment.getCommentContent());
            stmt.setObject(4, comment.getCreatedId());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                comment.setCommentId(keys.getInt(1));
            }
        }
        return comment;
    }

    public void delete(int commentId, int profileId) throws SQLException {
        String sql = "DELETE FROM comment WHERE comment_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.setInt(2, profileId);
            stmt.executeUpdate();
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setCommentId(rs.getInt("comment_id"));
        c.setActivityId(rs.getInt("activity_id"));
        c.setProfileId(rs.getInt("profile_id"));
        c.setCommentContent(rs.getString("comment_content"));
        c.setCreatedId((Integer) rs.getObject("created_id"));
        c.setDisplayName(rs.getString("display_name"));
        c.setProfilePic(rs.getString("profile_pic"));
        Date d = rs.getDate("created_date");
        if (d != null) c.setCreatedDate(d.toString());
        return c;
    }
}
