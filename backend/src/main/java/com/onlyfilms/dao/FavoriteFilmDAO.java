package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.FavoriteFilm;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriteFilmDAO {

    public List<FavoriteFilm> findByProfileId(int profileId) throws SQLException {
        String sql = """
            SELECT ff.favorite_film_id, ff.profile_id, ff.film_id, ff.sort_order,
                   f.film_title, f.poster_url, f.tmdb_id
            FROM favorite_film_list ff
            JOIN film f ON ff.film_id = f.film_id
            WHERE ff.profile_id = ?
            ORDER BY ff.sort_order ASC
            """;
        List<FavoriteFilm> favorites = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                favorites.add(mapRow(rs));
            }
        }
        return favorites;
    }

    public FavoriteFilm add(int profileId, int filmId) throws SQLException {
        // Get next sort order
        int sortOrder = 0;
        String countSql = "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM favorite_film_list WHERE profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) sortOrder = rs.getInt(1);
        }

        String sql = "INSERT INTO favorite_film_list (profile_id, film_id, sort_order) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, profileId);
            stmt.setInt(2, filmId);
            stmt.setInt(3, sortOrder);
            stmt.executeUpdate();

            FavoriteFilm fav = new FavoriteFilm();
            fav.setProfileId(profileId);
            fav.setFilmId(filmId);
            fav.setSortOrder(sortOrder);
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                fav.setFavoriteFilmId(keys.getInt(1));
            }
            return fav;
        }
    }

    public void remove(int profileId, int filmId) throws SQLException {
        String sql = "DELETE FROM favorite_film_list WHERE profile_id = ? AND film_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            stmt.setInt(2, filmId);
            stmt.executeUpdate();
        }
    }

    public boolean isFavorite(int profileId, int filmId) throws SQLException {
        String sql = "SELECT 1 FROM favorite_film_list WHERE profile_id = ? AND film_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            stmt.setInt(2, filmId);
            return stmt.executeQuery().next();
        }
    }

    private FavoriteFilm mapRow(ResultSet rs) throws SQLException {
        FavoriteFilm ff = new FavoriteFilm();
        ff.setFavoriteFilmId(rs.getInt("favorite_film_id"));
        ff.setProfileId(rs.getInt("profile_id"));
        ff.setFilmId(rs.getInt("film_id"));
        ff.setSortOrder(rs.getInt("sort_order"));
        ff.setFilmTitle(rs.getString("film_title"));
        ff.setPosterUrl(rs.getString("poster_url"));
        ff.setTmdbId((Integer) rs.getObject("tmdb_id"));
        return ff;
    }
}
