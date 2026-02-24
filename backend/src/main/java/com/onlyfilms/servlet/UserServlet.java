package com.onlyfilms.servlet;

import com.onlyfilms.dao.ProfileDAO;
import com.onlyfilms.model.Profile;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Handles user profile endpoints:
 * GET /api/users/me - current user's profile
 * GET /api/users/{profileId} - specific profile
 * GET /api/users/search?q=query - search profiles
 * PUT /api/users/me - update current user's profile
 */
public class UserServlet extends HttpServlet {
    private final ProfileDAO profileDAO = new ProfileDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/")) {
                // Search users
                String query = req.getParameter("q");
                if (query != null && !query.trim().isEmpty()) {
                    List<Profile> profiles = profileDAO.searchByDisplayName(query);
                    JsonUtil.writeSuccess(resp, profiles);
                } else {
                    JsonUtil.writeBadRequest(resp, "Provide a search query with ?q=...");
                }
            } else if (path.equals("/me")) {
                // Current user's profile
                Integer userId = (Integer) req.getAttribute("userId");
                if (userId == null) {
                    JsonUtil.writeUnauthorized(resp, "Not authenticated");
                    return;
                }
                Profile profile = profileDAO.findByUserId(userId);
                if (profile == null) {
                    JsonUtil.writeNotFound(resp, "Profile not found");
                    return;
                }
                JsonUtil.writeSuccess(resp, profile);
            } else if (path.equals("/search")) {
                String query = req.getParameter("q");
                if (query != null && !query.trim().isEmpty()) {
                    List<Profile> profiles = profileDAO.searchByDisplayName(query);
                    JsonUtil.writeSuccess(resp, profiles);
                } else {
                    JsonUtil.writeBadRequest(resp, "Search query required");
                }
            } else {
                // Get profile by ID
                try {
                    int profileId = Integer.parseInt(path.substring(1));
                    Profile profile = profileDAO.findById(profileId);
                    if (profile == null) {
                        JsonUtil.writeNotFound(resp, "Profile not found");
                        return;
                    }

                    // Check if current user follows this profile
                    Integer currentUserId = (Integer) req.getAttribute("userId");
                    if (currentUserId != null) {
                        Integer currentProfileId = (Integer) req.getAttribute("profileId");
                        if (currentProfileId != null && currentProfileId != profileId) {
                            com.onlyfilms.dao.FollowDAO followDAO = new com.onlyfilms.dao.FollowDAO();
                            profile.setIsFollowing(followDAO.isFollowing(currentProfileId, profileId));
                            profile.setIsFollowedBy(followDAO.isFollowing(profileId, currentProfileId));
                        }
                    }

                    JsonUtil.writeSuccess(resp, profile.toPublic());
                } catch (NumberFormatException e) {
                    JsonUtil.writeBadRequest(resp, "Invalid profile ID");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (!"/me".equals(path)) {
            JsonUtil.writeBadRequest(resp, "Can only update your own profile via /me");
            return;
        }

        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            Profile existing = profileDAO.findByUserId(userId);
            if (existing == null) {
                JsonUtil.writeNotFound(resp, "Profile not found");
                return;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            if (json.has("displayName")) existing.setDisplayName(json.get("displayName").getAsString());
            if (json.has("bio")) existing.setBio(json.get("bio").getAsString());
            if (json.has("favoriteMovie")) existing.setFavoriteMovie(json.get("favoriteMovie").getAsString());
            if (json.has("profilePic")) existing.setProfilePic(json.get("profilePic").getAsString());

            profileDAO.update(existing);

            Profile updated = profileDAO.findByUserId(userId);
            JsonUtil.writeSuccess(resp, "Profile updated", updated);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
