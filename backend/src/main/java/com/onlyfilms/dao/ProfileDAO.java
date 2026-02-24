package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Profile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO {

    public Profile findById(int profileId) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.user_id, p.display_name, p.bio, p.favorite_movie,
                   p.profile_pic, p.join_date_id, u.email,
                   d.full_date AS join_date
            FROM profiles p
            JOIN users u ON p.user_id = u.user_id
            LEFT JOIN date_dim d ON p.join_date_id = d.date_id
            WHERE p.profile_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowWithStats(conn, rs);
            }
        }
        return null;
    }

    public Profile findByUserId(int userId) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.user_id, p.display_name, p.bio, p.favorite_movie,
                   p.profile_pic, p.join_date_id, u.email,
                   d.full_date AS join_date
            FROM profiles p
            JOIN users u ON p.user_id = u.user_id
            LEFT JOIN date_dim d ON p.join_date_id = d.date_id
            WHERE p.user_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowWithStats(conn, rs);
            }
        }
        return null;
    }

    public Profile create(Profile profile) throws SQLException {
        // First ensure date_dim entry exists for today
        int dateId = getOrCreateDateId();

        String sql = "INSERT INTO profiles (user_id, display_name, bio, favorite_movie, profile_pic, join_date_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, profile.getUserId());
            stmt.setString(2, profile.getDisplayName());
            stmt.setString(3, profile.getBio());
            stmt.setString(4, profile.getFavoriteMovie());
            stmt.setString(5, profile.getProfilePic());
            stmt.setInt(6, dateId);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                profile.setProfileId(keys.getInt(1));
            }
            profile.setJoinDateId(dateId);
        }
        return profile;
    }

    public void update(Profile profile) throws SQLException {
        String sql = "UPDATE profiles SET display_name = ?, bio = ?, favorite_movie = ?, profile_pic = ? WHERE profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getDisplayName());
            stmt.setString(2, profile.getBio());
            stmt.setString(3, profile.getFavoriteMovie());
            stmt.setString(4, profile.getProfilePic());
            stmt.setInt(5, profile.getProfileId());
            stmt.executeUpdate();
        }
    }

    public List<Profile> searchByDisplayName(String query) throws SQLException {
        String sql = """
            SELECT p.profile_id, p.user_id, p.display_name, p.bio, p.favorite_movie,
                   p.profile_pic, p.join_date_id, u.email,
                   d.full_date AS join_date
            FROM profiles p
            JOIN users u ON p.user_id = u.user_id
            LEFT JOIN date_dim d ON p.join_date_id = d.date_id
            WHERE p.display_name LIKE ?
            LIMIT 20
            """;
        List<Profile> profiles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                profiles.add(mapRow(rs));
            }
        }
        return profiles;
    }

    private int getOrCreateDateId() throws SQLException {
        java.time.LocalDate today = java.time.LocalDate.now();
        String selectSql = "SELECT date_id FROM date_dim WHERE full_date = ?";
        String insertSql = "INSERT INTO date_dim (full_date, day, month, quarter, year) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setDate(1, java.sql.Date.valueOf(today));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("date_id");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDate(1, java.sql.Date.valueOf(today));
                stmt.setInt(2, today.getDayOfMonth());
                stmt.setInt(3, today.getMonthValue());
                stmt.setInt(4, (today.getMonthValue() - 1) / 3 + 1);
                stmt.setInt(5, today.getYear());
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to get or create date_dim entry");
    }

    private Profile mapRow(ResultSet rs) throws SQLException {
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
        if (joinDate != null) {
            p.setJoinDate(joinDate.toString());
        }
        return p;
    }

    private Profile mapRowWithStats(Connection conn, ResultSet rs) throws SQLException {
        Profile p = mapRow(rs);
        int profileId = p.getProfileId();

        // Count reviews (activities with a review)
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM activity WHERE profile_id = ? AND review_description IS NOT NULL")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setReviewCount(r.getInt(1));
        }
        // Count watched
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM activity WHERE profile_id = ? AND watched_status = 'watched'")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setWatchedCount(r.getInt(1));
        }
        // Count favorites
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM favorite_film_list WHERE profile_id = ?")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setFavoriteCount(r.getInt(1));
        }
        // Count lists
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM `list` WHERE profile_id = ?")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setListCount(r.getInt(1));
        }
        // Count followers
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM follows_list WHERE following_id = ?")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setFollowersCount(r.getInt(1));
        }
        // Count following
        try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM follows_list WHERE follower_id = ?")) {
            s.setInt(1, profileId);
            ResultSet r = s.executeQuery();
            if (r.next()) p.setFollowingCount(r.getInt(1));
        }

        return p;
    }
}
