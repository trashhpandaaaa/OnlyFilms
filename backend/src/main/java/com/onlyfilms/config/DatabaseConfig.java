package com.onlyfilms.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static String url;
    private static String username;
    private static String password;
    private static boolean initialized = false;
    
    public static void initialize() {
        if (initialized) return;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Properties props = new Properties();
            InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("db.properties");
            
            if (input != null) {
                props.load(input);
                url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/onlyfilms");
                username = props.getProperty("db.username", "root");
                password = props.getProperty("db.password", "");
            } else {
                url = "jdbc:mysql://localhost:3306/onlyfilms";
                username = "root";
                password = "";
            }
            
            initialized = true;
            System.out.println("Database configuration loaded successfully");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
    }
    
    static {
        initialize();
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
