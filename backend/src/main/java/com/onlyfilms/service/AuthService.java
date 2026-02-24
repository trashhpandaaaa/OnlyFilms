package com.onlyfilms.service;

import com.onlyfilms.dao.ProfileDAO;
import com.onlyfilms.dao.UserDAO;
import com.onlyfilms.model.Profile;
import com.onlyfilms.model.User;
import com.onlyfilms.util.JwtUtil;
import com.onlyfilms.util.PasswordUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();

    /**
     * Register a new user with email, password, and display name.
     * Creates both a user record (auth) and a profile record.
     */
    public Map<String, Object> register(String email, String password, String displayName) throws SQLException {
        // Validate input
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = email.split("@")[0]; // default display name from email
        }

        // Check if email already exists
        if (userDAO.emailExists(email.trim())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create user
        User user = new User(email.trim(), PasswordUtil.hashPassword(password));
        user = userDAO.create(user);

        // Create profile
        Profile profile = new Profile();
        profile.setUserId(user.getUserId());
        profile.setDisplayName(displayName.trim());
        profile = profileDAO.create(profile);

        // Generate JWT token
        String token = JwtUtil.generateToken(user.getUserId(), profile.getProfileId(), profile.getDisplayName());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("profileId", profile.getProfileId());
        result.put("email", user.getEmail());
        result.put("displayName", profile.getDisplayName());
        return result;
    }

    /**
     * Login with email and password.
     */
    public Map<String, Object> login(String email, String password) throws SQLException {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Get profile
        Profile profile = profileDAO.findByUserId(user.getUserId());
        if (profile == null) {
            throw new IllegalStateException("User profile not found");
        }

        // Generate JWT token
        String token = JwtUtil.generateToken(user.getUserId(), profile.getProfileId(), profile.getDisplayName());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("profileId", profile.getProfileId());
        result.put("email", user.getEmail());
        result.put("displayName", profile.getDisplayName());
        return result;
    }
}
