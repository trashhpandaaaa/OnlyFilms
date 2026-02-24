package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Film;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmDAO {

    public Film findById(int filmId) throws SQLException {
        String sql = "SELECT * FROM film WHERE film_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, filmId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public Film findByTmdbId(int tmdbId) throws SQLException {
        String sql = "SELECT * FROM film WHERE tmdb_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tmdbId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Find or create a film by TMDB ID. Used when a user creates an activity for a TMDB movie.
     */
    public Film findOrCreate(int tmdbId, String title, Integer releaseYear, String synopsis,
                             int runtimeMins, String posterUrl, String description) throws SQLException {
        Film existing = findByTmdbId(tmdbId);
        if (existing != null) return existing;

        Film film = new Film();
        film.setTmdbId(tmdbId);
        film.setFilmTitle(title);
        film.setReleaseYear(releaseYear);
        film.setSynopsis(synopsis);
        film.setRuntimeMins(runtimeMins);
        film.setPosterUrl(posterUrl);
        film.setFilmDescription(description);
        return create(film);
    }

    public Film create(Film film) throws SQLException {
        String sql = """
            INSERT INTO film (tmdb_id, film_title, release_year, synopsis, runtime_mins, poster_url, film_description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, film.getTmdbId());
            stmt.setString(2, film.getFilmTitle());
            stmt.setObject(3, film.getReleaseYear());
            stmt.setString(4, film.getSynopsis());
            stmt.setInt(5, film.getRuntimeMins());
            stmt.setString(6, film.getPosterUrl());
            stmt.setString(7, film.getFilmDescription());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                film.setFilmId(keys.getInt(1));
            }
        }
        return film;
    }

    public List<Film> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT * FROM film ORDER BY film_title LIMIT ? OFFSET ?";
        List<Film> films = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                films.add(mapRow(rs));
            }
        }
        return films;
    }

    public List<Film> search(String query) throws SQLException {
        String sql = "SELECT * FROM film WHERE film_title LIKE ? ORDER BY film_title LIMIT 20";
        List<Film> films = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                films.add(mapRow(rs));
            }
        }
        return films;
    }

    public Film getFilmWithStats(int filmId) throws SQLException {
        Film film = findById(filmId);
        if (film == null) return null;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Average rating
            try (PreparedStatement s = conn.prepareStatement("SELECT AVG(rating) as avg_rating FROM activity WHERE film_id = ? AND rating IS NOT NULL")) {
                s.setInt(1, filmId);
                ResultSet r = s.executeQuery();
                if (r.next()) {
                    double avg = r.getDouble("avg_rating");
                    film.setAverageRating(r.wasNull() ? null : Math.round(avg * 10.0) / 10.0);
                }
            }
            // Review count
            try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM activity WHERE film_id = ? AND review_description IS NOT NULL")) {
                s.setInt(1, filmId);
                ResultSet r = s.executeQuery();
                if (r.next()) film.setReviewCount(r.getInt(1));
            }
            // Watched count
            try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM activity WHERE film_id = ? AND watched_status = 'watched'")) {
                s.setInt(1, filmId);
                ResultSet r = s.executeQuery();
                if (r.next()) film.setWatchedCount(r.getInt(1));
            }
            // Favorite count
            try (PreparedStatement s = conn.prepareStatement("SELECT COUNT(*) FROM favorite_film_list WHERE film_id = ?")) {
                s.setInt(1, filmId);
                ResultSet r = s.executeQuery();
                if (r.next()) film.setFavoriteCount(r.getInt(1));
            }
        }
        return film;
    }

    private Film mapRow(ResultSet rs) throws SQLException {
        Film f = new Film();
        f.setFilmId(rs.getInt("film_id"));
        f.setTmdbId((Integer) rs.getObject("tmdb_id"));
        f.setFilmTitle(rs.getString("film_title"));
        f.setReleaseYear((Integer) rs.getObject("release_year"));
        f.setSynopsis(rs.getString("synopsis"));
        f.setRuntimeMins(rs.getInt("runtime_mins"));
        f.setPosterUrl(rs.getString("poster_url"));
        f.setFilmDescription(rs.getString("film_description"));
        return f;
    }
}
