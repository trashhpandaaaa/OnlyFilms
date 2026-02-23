package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.CustomList;
import com.onlyfilms.model.CustomList.ListItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Custom List operations
 */
public class CustomListDAO {

    /**
     * Find list by ID
     */
    public Optional<CustomList> findById(int id) {
        String sql = """
            SELECT cl.*, u.username
            FROM custom_lists cl
            JOIN users u ON cl.user_id = u.id
            WHERE cl.id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                CustomList list = mapResultSetToCustomList(rs);
                list.setItems(getListItems(id));
                return Optional.of(list);
            }
        } catch (SQLException e) {
            System.err.println("Error finding list by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Get user's lists
     */
    public List<CustomList> findByUserId(int userId, int page, int limit) {
        String sql = """
            SELECT cl.*, u.username,
                   (SELECT COUNT(*) FROM list_items li WHERE li.list_id = cl.id) as movie_count
            FROM custom_lists cl
            JOIN users u ON cl.user_id = u.id
            WHERE cl.user_id = ?
            ORDER BY cl.updated_at DESC
            LIMIT ? OFFSET ?
            """;
        List<CustomList> lists = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                CustomList list = mapResultSetToCustomList(rs);
                list.setMovieCount(rs.getInt("movie_count"));
                lists.add(list);
            }
        } catch (SQLException e) {
            System.err.println("Error finding lists by user: " + e.getMessage());
        }
        return lists;
    }

    /**
     * Get public lists (for discovery)
     */
    public List<CustomList> findPublicLists(int page, int limit) {
        String sql = """
            SELECT cl.*, u.username,
                   (SELECT COUNT(*) FROM list_items li WHERE li.list_id = cl.id) as movie_count
            FROM custom_lists cl
            JOIN users u ON cl.user_id = u.id
            WHERE cl.is_public = TRUE
            ORDER BY cl.updated_at DESC
            LIMIT ? OFFSET ?
            """;
        List<CustomList> lists = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, (page - 1) * limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                CustomList list = mapResultSetToCustomList(rs);
                list.setMovieCount(rs.getInt("movie_count"));
                lists.add(list);
            }
        } catch (SQLException e) {
            System.err.println("Error finding public lists: " + e.getMessage());
        }
        return lists;
    }

    /**
     * Create a new list
     */
    public CustomList save(CustomList list) throws SQLException {
        String sql = "INSERT INTO custom_lists (user_id, name, description, is_public) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, list.getUserId());
            stmt.setString(2, list.getName());
            stmt.setString(3, list.getDescription());
            stmt.setBoolean(4, list.isPublic());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating list failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    list.setId(generatedKeys.getInt(1));
                }
            }
        }
        return list;
    }

    /**
     * Update list details
     */
    public boolean update(CustomList list) {
        String sql = "UPDATE custom_lists SET name = ?, description = ?, is_public = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, list.getName());
            stmt.setString(2, list.getDescription());
            stmt.setBoolean(3, list.isPublic());
            stmt.setInt(4, list.getId());
            stmt.setInt(5, list.getUserId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating list: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete list
     */
    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM custom_lists WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting list: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add movie to list
     */
    public boolean addMovieToList(int listId, int movieId, int userId) {
        // First verify ownership
        if (!isOwner(listId, userId)) {
            return false;
        }

        String sql = """
            INSERT INTO list_items (list_id, movie_id, position)
            SELECT ?, ?, COALESCE(MAX(position), 0) + 1 FROM list_items WHERE list_id = ?
            ON DUPLICATE KEY UPDATE position = position
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            stmt.setInt(2, movieId);
            stmt.setInt(3, listId);
            
            boolean added = stmt.executeUpdate() > 0;
            if (added) {
                touchList(listId);
            }
            return added;
        } catch (SQLException e) {
            System.err.println("Error adding movie to list: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove movie from list
     */
    public boolean removeMovieFromList(int listId, int movieId, int userId) {
        // First verify ownership
        if (!isOwner(listId, userId)) {
            return false;
        }

        String sql = "DELETE FROM list_items WHERE list_id = ? AND movie_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            stmt.setInt(2, movieId);
            
            boolean removed = stmt.executeUpdate() > 0;
            if (removed) {
                touchList(listId);
            }
            return removed;
        } catch (SQLException e) {
            System.err.println("Error removing movie from list: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get items in a list
     */
    public List<ListItem> getListItems(int listId) {
        String sql = """
            SELECT li.*, m.title as movie_title, m.poster_url as movie_poster_url, m.release_year as movie_release_year
            FROM list_items li
            JOIN movies m ON li.movie_id = m.id
            WHERE li.list_id = ?
            ORDER BY li.position
            """;
        List<ListItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ListItem item = new ListItem();
                item.setMovieId(rs.getInt("movie_id"));
                item.setPosition(rs.getInt("position"));
                item.setAddedAt(rs.getTimestamp("added_at"));
                item.setMovieTitle(rs.getString("movie_title"));
                item.setMoviePosterUrl(rs.getString("movie_poster_url"));
                item.setMovieReleaseYear(rs.getObject("movie_release_year", Integer.class));
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error getting list items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Check if user owns the list
     */
    public boolean isOwner(int listId, int userId) {
        String sql = "SELECT COUNT(*) FROM custom_lists WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking list ownership: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if list is public or owned by user
     */
    public boolean canAccess(int listId, Integer userId) {
        String sql = "SELECT is_public, user_id FROM custom_lists WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                boolean isPublic = rs.getBoolean("is_public");
                int ownerId = rs.getInt("user_id");
                return isPublic || (userId != null && userId == ownerId);
            }
        } catch (SQLException e) {
            System.err.println("Error checking list access: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update list's updated_at timestamp
     */
    private void touchList(int listId) {
        String sql = "UPDATE custom_lists SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, listId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error touching list: " + e.getMessage());
        }
    }

    /**
     * Count user's lists
     */
    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM custom_lists WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting lists: " + e.getMessage());
        }
        return 0;
    }

    private CustomList mapResultSetToCustomList(ResultSet rs) throws SQLException {
        CustomList list = new CustomList();
        list.setId(rs.getInt("id"));
        list.setUserId(rs.getInt("user_id"));
        list.setName(rs.getString("name"));
        list.setDescription(rs.getString("description"));
        list.setPublic(rs.getBoolean("is_public"));
        list.setCreatedAt(rs.getTimestamp("created_at"));
        list.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        try {
            list.setUsername(rs.getString("username"));
        } catch (SQLException e) {
            // Column not in result set
        }
        
        return list;
    }
}
