package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Activity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityDAO {

    private static final String SELECT_WITH_JOINS = """
        SELECT a.activity_id, a.profile_id, a.film_id, a.review_date_id, a.watched_date_id,
               a.rating, a.watched_status, a.review_description,
               p.display_name, p.profile_pic,
               f.film_title, f.poster_url, f.tmdb_id,
               rd.full_date AS review_date, wd.full_date AS watched_date,
               (SELECT COUNT(*) FROM `like` l WHERE l.activity_id = a.activity_id) AS like_count,
               (SELECT COUNT(*) FROM comment c WHERE c.activity_id = a.activity_id) AS comment_count
        FROM activity a
        JOIN profiles p ON a.profile_id = p.profile_id
        JOIN film f ON a.film_id = f.film_id
        LEFT JOIN date_dim rd ON a.review_date_id = rd.date_id
        LEFT JOIN date_dim wd ON a.watched_date_id = wd.date_id
        """;

    public Activity findById(int activityId) throws SQLException {
        String sql = SELECT_WITH_JOINS + " WHERE a.activity_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public Activity create(Activity activity) throws SQLException {
        String sql = """
            INSERT INTO activity (profile_id, film_id, review_date_id, watched_date_id, rating, watched_status, review_description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, activity.getProfileId());
            stmt.setInt(2, activity.getFilmId());
            stmt.setObject(3, activity.getReviewDateId());
            stmt.setObject(4, activity.getWatchedDateId());
            stmt.setObject(5, activity.getRating());
            stmt.setString(6, activity.getWatchedStatus());
            stmt.setString(7, activity.getReviewDescription());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                activity.setActivityId(keys.getInt(1));
            }
        }
        return activity;
    }

    public void update(Activity activity) throws SQLException {
        String sql = """
            UPDATE activity SET rating = ?, watched_status = ?, review_description = ?,
                   review_date_id = ?, watched_date_id = ?
            WHERE activity_id = ? AND profile_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, activity.getRating());
            stmt.setString(2, activity.getWatchedStatus());
            stmt.setString(3, activity.getReviewDescription());
            stmt.setObject(4, activity.getReviewDateId());
            stmt.setObject(5, activity.getWatchedDateId());
            stmt.setInt(6, activity.getActivityId());
            stmt.setInt(7, activity.getProfileId());
            stmt.executeUpdate();
        }
    }

    public void delete(int activityId, int profileId) throws SQLException {
        String sql = "DELETE FROM activity WHERE activity_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, activityId);
            stmt.setInt(2, profileId);
            stmt.executeUpdate();
        }
    }

    /** Get activities for a specific film, ordered by most recent review date */
    public List<Activity> findByFilmId(int filmId, int limit, int offset) throws SQLException {
        String sql = SELECT_WITH_JOINS + " WHERE a.film_id = ? ORDER BY a.activity_id DESC LIMIT ? OFFSET ?";
        return executeList(sql, filmId, limit, offset);
    }

    /** Get activities for a specific profile */
    public List<Activity> findByProfileId(int profileId, int limit, int offset) throws SQLException {
        String sql = SELECT_WITH_JOINS + " WHERE a.profile_id = ? ORDER BY a.activity_id DESC LIMIT ? OFFSET ?";
        return executeList(sql, profileId, limit, offset);
    }

    /** Get recent activity feed (all users) */
    public List<Activity> getRecentActivity(int limit, int offset) throws SQLException {
        String sql = SELECT_WITH_JOINS + " ORDER BY a.activity_id DESC LIMIT ? OFFSET ?";
        List<Activity> activities = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activities.add(mapRow(rs));
            }
        }
        return activities;
    }

    /** Get activity feed from followed users */
    public List<Activity> getFeedForProfile(int profileId, int limit, int offset) throws SQLException {
        String sql = SELECT_WITH_JOINS +
            " WHERE a.profile_id IN (SELECT following_id FROM follows_list WHERE follower_id = ?)" +
            " ORDER BY a.activity_id DESC LIMIT ? OFFSET ?";
        return executeList(sql, profileId, limit, offset);
    }

    /** Check if a profile already has an activity for a film */
    public Activity findByProfileAndFilm(int profileId, int filmId) throws SQLException {
        String sql = SELECT_WITH_JOINS + " WHERE a.profile_id = ? AND a.film_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            stmt.setInt(2, filmId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    private List<Activity> executeList(String sql, int paramId, int limit, int offset) throws SQLException {
        List<Activity> activities = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paramId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activities.add(mapRow(rs));
            }
        }
        return activities;
    }

    private Activity mapRow(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setActivityId(rs.getInt("activity_id"));
        a.setProfileId(rs.getInt("profile_id"));
        a.setFilmId(rs.getInt("film_id"));
        a.setReviewDateId((Integer) rs.getObject("review_date_id"));
        a.setWatchedDateId((Integer) rs.getObject("watched_date_id"));
        double rating = rs.getDouble("rating");
        a.setRating(rs.wasNull() ? null : rating);
        a.setWatchedStatus(rs.getString("watched_status"));
        a.setReviewDescription(rs.getString("review_description"));
        a.setDisplayName(rs.getString("display_name"));
        a.setProfilePic(rs.getString("profile_pic"));
        a.setFilmTitle(rs.getString("film_title"));
        a.setPosterUrl(rs.getString("poster_url"));
        a.setTmdbId((Integer) rs.getObject("tmdb_id"));
        Date rd = rs.getDate("review_date");
        if (rd != null) a.setReviewDate(rd.toString());
        Date wd = rs.getDate("watched_date");
        if (wd != null) a.setWatchedDate(wd.toString());
        a.setLikeCount(rs.getInt("like_count"));
        a.setCommentCount(rs.getInt("comment_count"));
        return a;
    }
}
