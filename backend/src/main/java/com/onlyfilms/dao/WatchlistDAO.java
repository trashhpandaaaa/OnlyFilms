package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.WatchHistory;
import com.onlyfilms.model.WatchlistItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Watchlist and Watch History operations
 */
public class WatchlistDAO {

    // ==================== WATCHLIST ====================

    /**
     * Get user's watchlist
     */
    public List<WatchlistItem> getWatchlist(int userId, int page, int limit) {
        String sql = """
            SELECT w.*, m.title as movie_title, m.poster_url as movie_poster_url, m.release_year as movie_release_year
            FROM watchlist w
            JOIN movies m ON w.movie_id = m.id
            WHERE w.user_id = ?
            ORDER BY w.added_at DESC
            LIMIT ? OFFSET ?
            """;
        List<WatchlistItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                items.add(mapResultSetToWatchlistItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting watchlist: " + e.getMessage());
        }
        return items;
    }

    /**
     * Add movie to watchlist
     */
    public boolean addToWatchlist(int userId, int movieId) {
        String sql = "INSERT IGNORE INTO watchlist (user_id, movie_id) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding to watchlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove movie from watchlist
     */
    public boolean removeFromWatchlist(int userId, int movieId) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error removing from watchlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if movie is in watchlist
     */
    public boolean isInWatchlist(int userId, int movieId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ? AND movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking watchlist: " + e.getMessage());
        }
        return false;
    }

    /**
     * Count watchlist items
     */
    public int countWatchlist(int userId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting watchlist: " + e.getMessage());
        }
        return 0;
    }

    // ==================== WATCH HISTORY ====================

    /**
     * Get user's watch history
     */
    public List<WatchHistory> getWatchHistory(int userId, int page, int limit) {
        String sql = """
            SELECT wh.*, m.title as movie_title, m.poster_url as movie_poster_url, m.release_year as movie_release_year
            FROM watch_history wh
            JOIN movies m ON wh.movie_id = m.id
            WHERE wh.user_id = ?
            ORDER BY wh.watched_at DESC, wh.created_at DESC
            LIMIT ? OFFSET ?
            """;
        List<WatchHistory> history = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                history.add(mapResultSetToWatchHistory(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting watch history: " + e.getMessage());
        }
        return history;
    }

    /**
     * Log a watched movie
     */
    public WatchHistory logWatch(int userId, int movieId, Date watchedAt, boolean isRewatch) throws SQLException {
        String sql = "INSERT INTO watch_history (user_id, movie_id, watched_at, is_rewatch) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            stmt.setDate(3, watchedAt);
            stmt.setBoolean(4, isRewatch);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Logging watch failed, no rows affected.");
            }
            
            WatchHistory history = new WatchHistory(userId, movieId, watchedAt, isRewatch);
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    history.setId(generatedKeys.getInt(1));
                }
            }
            
            // Remove from watchlist if present
            removeFromWatchlist(userId, movieId);
            
            return history;
        }
    }

    /**
     * Delete watch history entry
     */
    public boolean deleteWatchHistory(int id, int userId) {
        String sql = "DELETE FROM watch_history WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting watch history: " + e.getMessage());
            return false;
        }
    }

    /**
     * Count watched movies for user
     */
    public int countWatched(int userId) {
        String sql = "SELECT COUNT(DISTINCT movie_id) FROM watch_history WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting watched: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Check if user has watched a movie
     */
    public boolean hasWatched(int userId, int movieId) {
        String sql = "SELECT COUNT(*) FROM watch_history WHERE user_id = ? AND movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking watch history: " + e.getMessage());
        }
        return false;
    }

    // ==================== MAPPERS ====================

    private WatchlistItem mapResultSetToWatchlistItem(ResultSet rs) throws SQLException {
        WatchlistItem item = new WatchlistItem();
        item.setUserId(rs.getInt("user_id"));
        item.setMovieId(rs.getInt("movie_id"));
        item.setAddedAt(rs.getTimestamp("added_at"));
        item.setMovieTitle(rs.getString("movie_title"));
        item.setMoviePosterUrl(rs.getString("movie_poster_url"));
        item.setMovieReleaseYear(rs.getObject("movie_release_year", Integer.class));
        return item;
    }

    private WatchHistory mapResultSetToWatchHistory(ResultSet rs) throws SQLException {
        WatchHistory history = new WatchHistory();
        history.setId(rs.getInt("id"));
        history.setUserId(rs.getInt("user_id"));
        history.setMovieId(rs.getInt("movie_id"));
        history.setWatchedAt(rs.getDate("watched_at"));
        history.setRewatch(rs.getBoolean("is_rewatch"));
        history.setCreatedAt(rs.getTimestamp("created_at"));
        history.setMovieTitle(rs.getString("movie_title"));
        history.setMoviePosterUrl(rs.getString("movie_poster_url"));
        history.setMovieReleaseYear(rs.getObject("movie_release_year", Integer.class));
        return history;
    }
}
