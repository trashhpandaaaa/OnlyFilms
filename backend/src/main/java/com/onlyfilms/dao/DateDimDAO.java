package com.onlyfilms.dao;

import com.onlyfilms.config.DatabaseConfig;

import java.sql.*;
import java.time.LocalDate;

/**
 * DAO for the date_dim table. Provides utility methods to get or create date entries.
 */
public class DateDimDAO {

    /**
     * Get or create a date_dim entry for the given date. Returns the date_id.
     */
    public int getOrCreateDateId(LocalDate date) throws SQLException {
        String selectSql = "SELECT date_id FROM date_dim WHERE full_date = ?";
        String insertSql = "INSERT INTO date_dim (full_date, day, month, quarter, year) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setDate(1, java.sql.Date.valueOf(date));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("date_id");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDate(1, java.sql.Date.valueOf(date));
                stmt.setInt(2, date.getDayOfMonth());
                stmt.setInt(3, date.getMonthValue());
                stmt.setInt(4, (date.getMonthValue() - 1) / 3 + 1);
                stmt.setInt(5, date.getYear());
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to get or create date_dim entry for " + date);
    }

    /**
     * Get or create a date_dim entry for today.
     */
    public int getOrCreateToday() throws SQLException {
        return getOrCreateDateId(LocalDate.now());
    }
}
