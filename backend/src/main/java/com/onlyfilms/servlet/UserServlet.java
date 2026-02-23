package com.onlyfilms.servlet;

import com.onlyfilms.dao.FollowDAO;
import com.onlyfilms.model.User;
import com.onlyfilms.model.UserProfile;
import com.onlyfilms.service.UserService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User Profile and Social endpoints
 * GET    /api/users/me                 - Get current user profile (auth)
 * GET    /api/users/{id}               - Get user profile by ID
 * GET    /api/users/username/{name}    - Get user profile by username
 * PUT    /api/users/me                 - Update current user profile (auth)
 * GET    /api/users/search             - Search users
 * GET    /api/users/{id}/followers     - Get user's followers
 * GET    /api/users/{id}/following     - Get users they follow
 * POST   /api/users/{id}/follow        - Follow user (auth)
 * DELETE /api/users/{id}/follow        - Unfollow user (auth)
 */
public class UserServlet extends HttpServlet {
    
    private final UserService userService;
    private final FollowDAO followDAO;

    public UserServlet() {
        this.userService = new UserService();
        this.followDAO = new FollowDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer currentUserId = (Integer) req.getAttribute("userId");
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Endpoint requires path");
            return;
        }

        if (pathInfo.equals("/me")) {
            // GET /api/users/me
            handleGetCurrentUser(req, resp, currentUserId);
        } else if (pathInfo.equals("/search")) {
            // GET /api/users/search
            handleSearchUsers(req, resp);
        } else if (pathInfo.startsWith("/username/")) {
            // GET /api/users/username/{name}
            handleGetByUsername(req, resp, pathInfo, currentUserId);
        } else if (pathInfo.contains("/followers")) {
            // GET /api/users/{id}/followers
            handleGetFollowers(req, resp, pathInfo);
        } else if (pathInfo.contains("/following")) {
            // GET /api/users/{id}/following
            handleGetFollowing(req, resp, pathInfo);
        } else {
            // GET /api/users/{id}
            handleGetById(req, resp, pathInfo, currentUserId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer currentUserId = (Integer) req.getAttribute("userId");
        if (currentUserId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.contains("/follow")) {
            JsonUtil.writeNotFound(resp, "Endpoint not found");
            return;
        }

        // POST /api/users/{id}/follow
        handleFollow(req, resp, pathInfo, currentUserId);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer currentUserId = (Integer) req.getAttribute("userId");
        if (currentUserId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.equals("/me")) {
            JsonUtil.writeForbidden(resp, "Can only update your own profile");
            return;
        }

        // PUT /api/users/me
        handleUpdateProfile(req, resp, currentUserId);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer currentUserId = (Integer) req.getAttribute("userId");
        if (currentUserId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.contains("/follow")) {
            JsonUtil.writeNotFound(resp, "Endpoint not found");
            return;
        }

        // DELETE /api/users/{id}/follow
        handleUnfollow(req, resp, pathInfo, currentUserId);
    }

    /**
     * GET /api/users/me
     */
    private void handleGetCurrentUser(HttpServletRequest req, HttpServletResponse resp, Integer currentUserId) throws IOException {
        if (currentUserId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        Optional<UserProfile> profile = userService.getProfile(currentUserId, currentUserId);
        
        if (profile.isPresent()) {
            JsonUtil.writeSuccess(resp, profile.get());
        } else {
            JsonUtil.writeNotFound(resp, "User not found");
        }
    }

    /**
     * GET /api/users/{id}
     */
    private void handleGetById(HttpServletRequest req, HttpServletResponse resp, String pathInfo, Integer currentUserId) throws IOException {
        try {
            String idPart = pathInfo.substring(1);
            if (idPart.contains("/")) {
                idPart = idPart.substring(0, idPart.indexOf("/"));
            }
            int userId = Integer.parseInt(idPart);

            Optional<UserProfile> profile = userService.getProfile(userId, currentUserId);
            
            if (profile.isPresent()) {
                JsonUtil.writeSuccess(resp, profile.get());
            } else {
                JsonUtil.writeNotFound(resp, "User not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * GET /api/users/username/{name}
     */
    private void handleGetByUsername(HttpServletRequest req, HttpServletResponse resp, String pathInfo, Integer currentUserId) throws IOException {
        String username = pathInfo.substring(10); // "/username/".length() = 10
        
        if (username.isEmpty()) {
            JsonUtil.writeBadRequest(resp, "Username required");
            return;
        }

        Optional<UserProfile> profile = userService.getProfileByUsername(username, currentUserId);
        
        if (profile.isPresent()) {
            JsonUtil.writeSuccess(resp, profile.get());
        } else {
            JsonUtil.writeNotFound(resp, "User not found");
        }
    }

    /**
     * PUT /api/users/me
     */
    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp, int currentUserId) throws IOException {
        ProfileUpdateRequest request = JsonUtil.fromRequest(req, ProfileUpdateRequest.class);

        if (request == null) {
            JsonUtil.writeBadRequest(resp, "Invalid request body");
            return;
        }

        if (userService.updateProfile(currentUserId, request.bio, request.avatarUrl)) {
            Optional<UserProfile> profile = userService.getProfile(currentUserId, currentUserId);
            JsonUtil.writeSuccess(resp, "Profile updated", profile.orElse(null));
        } else {
            JsonUtil.writeServerError(resp, "Failed to update profile");
        }
    }

    /**
     * GET /api/users/search
     */
    private void handleSearchUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        
        if (query == null || query.trim().isEmpty()) {
            JsonUtil.writeBadRequest(resp, "Search query 'q' is required");
            return;
        }

        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        List<User> users = followDAO.searchUsers(query.trim(), page, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("users", users.stream().map(User::toDTO).toList());
        result.put("query", query);
        result.put("page", page);
        result.put("limit", limit);

        JsonUtil.writeSuccess(resp, result);
    }

    /**
     * GET /api/users/{id}/followers
     */
    private void handleGetFollowers(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int userId = extractUserId(pathInfo);
            int page = getIntParam(req, "page", 1);
            int limit = getIntParam(req, "limit", 20);

            List<User> followers = followDAO.getFollowers(userId, page, limit);
            int total = followDAO.countFollowers(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("followers", followers.stream().map(User::toDTO).toList());
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", total);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * GET /api/users/{id}/following
     */
    private void handleGetFollowing(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int userId = extractUserId(pathInfo);
            int page = getIntParam(req, "page", 1);
            int limit = getIntParam(req, "limit", 20);

            List<User> following = followDAO.getFollowing(userId, page, limit);
            int total = followDAO.countFollowing(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("following", following.stream().map(User::toDTO).toList());
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", total);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * POST /api/users/{id}/follow
     */
    private void handleFollow(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int currentUserId) throws IOException {
        try {
            int targetUserId = extractUserId(pathInfo);

            if (targetUserId == currentUserId) {
                JsonUtil.writeBadRequest(resp, "Cannot follow yourself");
                return;
            }

            if (followDAO.follow(currentUserId, targetUserId)) {
                JsonUtil.writeCreated(resp, Map.of(
                    "message", "Now following user",
                    "userId", targetUserId
                ));
            } else {
                JsonUtil.writeBadRequest(resp, "Already following or user not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * DELETE /api/users/{id}/follow
     */
    private void handleUnfollow(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int currentUserId) throws IOException {
        try {
            int targetUserId = extractUserId(pathInfo);

            if (followDAO.unfollow(currentUserId, targetUserId)) {
                JsonUtil.writeSuccess(resp, "Unfollowed user", null);
            } else {
                JsonUtil.writeNotFound(resp, "Not following this user");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    private int extractUserId(String pathInfo) {
        // Path like /123/followers or /123/following or /123/follow
        String idPart = pathInfo.substring(1); // Remove leading /
        if (idPart.contains("/")) {
            idPart = idPart.substring(0, idPart.indexOf("/"));
        }
        return Integer.parseInt(idPart);
    }

    private int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // Request DTO
    private static class ProfileUpdateRequest {
        String bio;
        String avatarUrl;
    }
}
