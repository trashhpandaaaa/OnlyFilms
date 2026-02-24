package com.onlyfilms.servlet;

import com.onlyfilms.dao.FollowDAO;
import com.onlyfilms.model.Profile;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles follow endpoints:
 * GET    /api/follows/{profileId}/followers - get followers
 * GET    /api/follows/{profileId}/following - get following
 * POST   /api/follows/{profileId}          - follow a user (auth required)
 * DELETE  /api/follows/{profileId}          - unfollow a user (auth required)
 */
public class FollowServlet extends HttpServlet {
    private final FollowDAO followDAO = new FollowDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/")) {
                JsonUtil.writeBadRequest(resp, "Profile ID required");
                return;
            }

            if (path.endsWith("/followers")) {
                int profileId = Integer.parseInt(path.substring(1, path.indexOf("/followers")));
                List<Profile> followers = followDAO.getFollowers(profileId);
                JsonUtil.writeSuccess(resp, followers);
            } else if (path.endsWith("/following")) {
                int profileId = Integer.parseInt(path.substring(1, path.indexOf("/following")));
                List<Profile> following = followDAO.getFollowing(profileId);
                JsonUtil.writeSuccess(resp, following);
            } else {
                // Check follow status
                Integer currentProfileId = (Integer) req.getAttribute("profileId");
                int targetProfileId = Integer.parseInt(path.substring(1));
                if (currentProfileId != null) {
                    boolean isFollowing = followDAO.isFollowing(currentProfileId, targetProfileId);
                    Map<String, Object> result = new HashMap<>();
                    result.put("isFollowing", isFollowing);
                    result.put("followerCount", followDAO.getFollowerCount(targetProfileId));
                    result.put("followingCount", followDAO.getFollowingCount(targetProfileId));
                    JsonUtil.writeSuccess(resp, result);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("followerCount", followDAO.getFollowerCount(targetProfileId));
                    result.put("followingCount", followDAO.getFollowingCount(targetProfileId));
                    JsonUtil.writeSuccess(resp, result);
                }
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid profile ID");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Profile ID required");
            return;
        }

        Integer currentProfileId = (Integer) req.getAttribute("profileId");
        if (currentProfileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int targetProfileId = Integer.parseInt(path.substring(1));
            if (currentProfileId == targetProfileId) {
                JsonUtil.writeBadRequest(resp, "Cannot follow yourself");
                return;
            }
            boolean followed = followDAO.follow(currentProfileId, targetProfileId);
            Map<String, Object> result = new HashMap<>();
            result.put("following", followed);
            result.put("followerCount", followDAO.getFollowerCount(targetProfileId));
            JsonUtil.writeSuccess(resp, followed ? "Followed" : "Already following", result);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Profile ID required");
            return;
        }

        Integer currentProfileId = (Integer) req.getAttribute("profileId");
        if (currentProfileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int targetProfileId = Integer.parseInt(path.substring(1));
            followDAO.unfollow(currentProfileId, targetProfileId);
            Map<String, Object> result = new HashMap<>();
            result.put("following", false);
            result.put("followerCount", followDAO.getFollowerCount(targetProfileId));
            JsonUtil.writeSuccess(resp, "Unfollowed", result);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
