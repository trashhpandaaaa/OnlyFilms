package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Genre;
import com.onlyfilms.model.Movie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieDAO {
    private final GenreDAO genreDAO = new GenreDAO();
    
    /**
     * Ensure a movie with the given TMDB ID exists in the database.
     * If it doesn't exist, create a placeholder entry.
     * This is needed for reviews/watchlist to work with TMDB movies.
     */
    public void ensureMovieExists(int tmdbId, String title, String posterUrl) {
        String checkSql = "SELECT id FROM movies WHERE id = ?";
        String insertSql = "INSERT INTO movies (id, title, poster_url) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE title = VALUES(title)";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check if exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, tmdbId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return; // Already exists
                }
            }
            
            // Insert if not exists
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, tmdbId);
                insertStmt.setString(2, title != null ? title : "Unknown");
                insertStmt.setString(3, posterUrl);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring movie exists: " + e.getMessage());
        }
    }
    
    public List<Movie> findAll(int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movie movie = mapResultSetToMovie(rs);
                movie.setGenres(genreDAO.findByMovieId(movie.getId()));
                movies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }
    
    public Optional<Movie> findById(int id) {
        String sql = "SELECT * FROM movies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Movie movie = mapResultSetToMovie(rs);
                movie.setGenres(genreDAO.findByMovieId(movie.getId()));
                return Optional.of(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    public List<Movie> search(String query, int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE title LIKE ? OR original_title LIKE ? ORDER BY average_rating DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, pageSize);
            stmt.setInt(4, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movie movie = mapResultSetToMovie(rs);
                movie.setGenres(genreDAO.findByMovieId(movie.getId()));
                movies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }
    
    public List<Movie> findByGenre(int genreId, int page, int pageSize) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT m.* FROM movies m " +
                     "JOIN movie_genres mg ON m.id = mg.movie_id " +
                     "WHERE mg.genre_id = ? ORDER BY m.average_rating DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, genreId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movie movie = mapResultSetToMovie(rs);
                movie.setGenres(genreDAO.findByMovieId(movie.getId()));
                movies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }
    
    public List<Movie> findTopRated(int limit) {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE rating_count > 0 ORDER BY average_rating DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movie movie = mapResultSetToMovie(rs);
                movie.setGenres(genreDAO.findByMovieId(movie.getId()));
                movies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }
    
    public Movie create(Movie movie) {
        String sql = "INSERT INTO movies (title, original_title, overview, poster_url, backdrop_url, release_date, runtime, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, movie.getTitle());
            stmt.setString(2, movie.getOriginalTitle());
            stmt.setString(3, movie.getOverview());
            stmt.setString(4, movie.getPosterUrl());
            stmt.setString(5, movie.getBackdropUrl());
            stmt.setDate(6, movie.getReleaseDate() != null ? Date.valueOf(movie.getReleaseDate()) : null);
            stmt.setInt(7, movie.getRuntime());
            stmt.setString(8, movie.getLanguage());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    movie.setId(generatedKeys.getInt(1));
                    
                    // Add genres
                    if (movie.getGenres() != null) {
                        for (Genre genre : movie.getGenres()) {
                            addGenreToMovie(movie.getId(), genre.getId());
                        }
                    }
                    return movie;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void addGenreToMovie(int movieId, int genreId) {
        String sql = "INSERT IGNORE INTO movie_genres (movie_id, genre_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            stmt.setInt(2, genreId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateRating(int movieId) {
        String sql = "UPDATE movies SET average_rating = (SELECT AVG(rating) FROM reviews WHERE movie_id = ?), " +
                     "rating_count = (SELECT COUNT(*) FROM reviews WHERE movie_id = ?) WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            stmt.setInt(2, movieId);
            stmt.setInt(3, movieId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateRating(int movieId, double avgRating, int ratingCount) {
        String sql = "UPDATE movies SET average_rating = ?, rating_count = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, avgRating);
            stmt.setInt(2, ratingCount);
            stmt.setInt(3, movieId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public int count() {
        String sql = "SELECT COUNT(*) FROM movies";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean existsByTitle(String title) {
        String sql = "SELECT 1 FROM movies WHERE title = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public Movie save(Movie movie) throws SQLException {
        String sql = "INSERT INTO movies (title, original_title, overview, poster_url, backdrop_url, release_date, runtime, language, average_rating, rating_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, movie.getTitle());
            stmt.setString(2, movie.getOriginalTitle());
            stmt.setString(3, movie.getOverview());
            stmt.setString(4, movie.getPosterUrl());
            stmt.setString(5, movie.getBackdropUrl());
            stmt.setDate(6, movie.getReleaseDate() != null ? Date.valueOf(movie.getReleaseDate()) : null);
            stmt.setInt(7, movie.getRuntime());
            stmt.setString(8, movie.getLanguage());
            stmt.setDouble(9, movie.getAverageRating());
            stmt.setInt(10, movie.getRatingCount());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    movie.setId(generatedKeys.getInt(1));
                }
            }
        }
        return movie;
    }
    
    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setId(rs.getInt("id"));
        movie.setTitle(rs.getString("title"));
        movie.setOriginalTitle(rs.getString("original_title"));
        movie.setOverview(rs.getString("overview"));
        movie.setPosterUrl(rs.getString("poster_url"));
        movie.setBackdropUrl(rs.getString("backdrop_url"));
        Date releaseDate = rs.getDate("release_date");
        if (releaseDate != null) movie.setReleaseDate(releaseDate.toLocalDate());
        movie.setRuntime(rs.getInt("runtime"));
        movie.setLanguage(rs.getString("language"));
        movie.setAverageRating(rs.getDouble("average_rating"));
        movie.setRatingCount(rs.getInt("rating_count"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) movie.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) movie.setUpdatedAt(updatedAt.toLocalDateTime());
        return movie;
    }
}
