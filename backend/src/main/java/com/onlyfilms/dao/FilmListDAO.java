package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.model.Film;
import com.onlyfilms.model.FilmList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmListDAO {

    public FilmList findById(int listId) throws SQLException {
        String sql = """
            SELECT l.list_id, l.profile_id, l.list_name, l.list_description, l.created_date_id, l.is_public,
                   p.display_name, p.profile_pic, d.full_date AS created_date,
                   (SELECT COUNT(*) FROM list_profile lp WHERE lp.list_id = l.list_id) AS film_count
            FROM `list` l
            JOIN profiles p ON l.profile_id = p.profile_id
            LEFT JOIN date_dim d ON l.created_date_id = d.date_id
            WHERE l.list_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                FilmList list = mapRow(rs);
                list.setFilms(getFilmsInList(conn, listId));
                return list;
            }
        }
        return null;
    }

    public List<FilmList> findByProfileId(int profileId) throws SQLException {
        String sql = """
            SELECT l.list_id, l.profile_id, l.list_name, l.list_description, l.created_date_id, l.is_public,
                   p.display_name, p.profile_pic, d.full_date AS created_date,
                   (SELECT COUNT(*) FROM list_profile lp WHERE lp.list_id = l.list_id) AS film_count
            FROM `list` l
            JOIN profiles p ON l.profile_id = p.profile_id
            LEFT JOIN date_dim d ON l.created_date_id = d.date_id
            WHERE l.profile_id = ?
            ORDER BY l.list_id DESC
            """;
        List<FilmList> lists = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lists.add(mapRow(rs));
            }
        }
        return lists;
    }

    public List<FilmList> getPublicLists(int limit, int offset) throws SQLException {
        String sql = """
            SELECT l.list_id, l.profile_id, l.list_name, l.list_description, l.created_date_id, l.is_public,
                   p.display_name, p.profile_pic, d.full_date AS created_date,
                   (SELECT COUNT(*) FROM list_profile lp WHERE lp.list_id = l.list_id) AS film_count
            FROM `list` l
            JOIN profiles p ON l.profile_id = p.profile_id
            LEFT JOIN date_dim d ON l.created_date_id = d.date_id
            WHERE l.is_public = TRUE
            ORDER BY l.list_id DESC
            LIMIT ? OFFSET ?
            """;
        List<FilmList> lists = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lists.add(mapRow(rs));
            }
        }
        return lists;
    }

    public FilmList create(FilmList list) throws SQLException {
        String sql = "INSERT INTO `list` (profile_id, list_name, list_description, created_date_id, is_public) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, list.getProfileId());
            stmt.setString(2, list.getListName());
            stmt.setString(3, list.getListDescription());
            stmt.setObject(4, list.getCreatedDateId());
            stmt.setBoolean(5, list.getIsPublic());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                list.setListId(keys.getInt(1));
            }
        }
        return list;
    }

    public void update(FilmList list) throws SQLException {
        String sql = "UPDATE `list` SET list_name = ?, list_description = ?, is_public = ? WHERE list_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, list.getListName());
            stmt.setString(2, list.getListDescription());
            stmt.setBoolean(3, list.getIsPublic());
            stmt.setInt(4, list.getListId());
            stmt.setInt(5, list.getProfileId());
            stmt.executeUpdate();
        }
    }

    public void delete(int listId, int profileId) throws SQLException {
        String sql = "DELETE FROM `list` WHERE list_id = ? AND profile_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            stmt.setInt(2, profileId);
            stmt.executeUpdate();
        }
    }

    public void addFilmToList(int listId, int filmId) throws SQLException {
        String sql = "INSERT IGNORE INTO list_profile (list_id, film_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            stmt.setInt(2, filmId);
            stmt.executeUpdate();
        }
    }

    public void removeFilmFromList(int listId, int filmId) throws SQLException {
        String sql = "DELETE FROM list_profile WHERE list_id = ? AND film_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            stmt.setInt(2, filmId);
            stmt.executeUpdate();
        }
    }

    private List<Film> getFilmsInList(Connection conn, int listId) throws SQLException {
        String sql = """
            SELECT f.film_id, f.tmdb_id, f.film_title, f.release_year, f.synopsis,
                   f.runtime_mins, f.poster_url, f.film_description
            FROM list_profile lp
            JOIN film f ON lp.film_id = f.film_id
            WHERE lp.list_id = ?
            """;
        List<Film> films = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Film f = new Film();
                f.setFilmId(rs.getInt("film_id"));
                f.setTmdbId((Integer) rs.getObject("tmdb_id"));
                f.setFilmTitle(rs.getString("film_title"));
                f.setReleaseYear((Integer) rs.getObject("release_year"));
                f.setSynopsis(rs.getString("synopsis"));
                f.setRuntimeMins(rs.getInt("runtime_mins"));
                f.setPosterUrl(rs.getString("poster_url"));
                f.setFilmDescription(rs.getString("film_description"));
                films.add(f);
            }
        }
        return films;
    }

    private FilmList mapRow(ResultSet rs) throws SQLException {
        FilmList l = new FilmList();
        l.setListId(rs.getInt("list_id"));
        l.setProfileId(rs.getInt("profile_id"));
        l.setListName(rs.getString("list_name"));
        l.setListDescription(rs.getString("list_description"));
        l.setCreatedDateId((Integer) rs.getObject("created_date_id"));
        l.setIsPublic(rs.getBoolean("is_public"));
        l.setDisplayName(rs.getString("display_name"));
        l.setProfilePic(rs.getString("profile_pic"));
        Date d = rs.getDate("created_date");
        if (d != null) l.setCreatedDate(d.toString());
        l.setFilmCount(rs.getInt("film_count"));
        return l;
    }
}
