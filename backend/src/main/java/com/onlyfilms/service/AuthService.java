package com.onlyfilms.service;

import com.onlyfilms.dao.UserDAO;
import com.onlyfilms.model.User;
import com.onlyfilms.util.JwtUtil;
import com.onlyfilms.util.PasswordUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    
    public Map<String, Object> register(String username, String email, String password) {
        Map<String, Object> result = new HashMap<>();
        
        // Validate input
        if (username == null || username.trim().length() < 3) {
            result.put("success", false);
            result.put("message", "Username must be at least 3 characters");
            return result;
        }
        
        if (email == null || !email.contains("@")) {
            result.put("success", false);
            result.put("message", "Invalid email address");
            return result;
        }
        
        if (password == null || password.length() < 6) {
            result.put("success", false);
            result.put("message", "Password must be at least 6 characters");
            return result;
        }
        
        // Check if username exists
        if (userDAO.existsByUsername(username)) {
            result.put("success", false);
            result.put("message", "Username already taken");
            return result;
        }
        
        // Check if email exists
        if (userDAO.existsByEmail(email)) {
            result.put("success", false);
            result.put("message", "Email already registered");
            return result;
        }
        
        // Create user
        User user = new User(username, email, PasswordUtil.hashPassword(password));
        user = userDAO.create(user);
        
        if (user != null) {
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            result.put("success", true);
            result.put("message", "Registration successful");
            result.put("token", token);
            result.put("user", user.toPublicUser());
        } else {
            result.put("success", false);
            result.put("message", "Failed to create user");
        }
        
        return result;
    }
    
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isEmpty()) {
            // Try email
            userOpt = userDAO.findByEmail(username);
        }
        
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid credentials");
            return result;
        }
        
        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            result.put("success", false);
            result.put("message", "Invalid credentials");
            return result;
        }
        
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        result.put("success", true);
        result.put("message", "Login successful");
        result.put("token", token);
        result.put("user", user.toPublicUser());
        
        return result;
    }
    
    public Optional<User> getCurrentUser(int userId) {
        return userDAO.findById(userId);
    }
}
