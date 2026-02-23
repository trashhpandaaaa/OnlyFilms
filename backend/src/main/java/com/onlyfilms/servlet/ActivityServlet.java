package com.onlyfilms.servlet;

import com.onlyfilms.dao.ActivityDAO;
import com.onlyfilms.model.Activity;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity Feed endpoints
 * GET /api/activity/feed     - Get feed from followed users (auth)
 * GET /api/activity/user/{id} - Get activity for specific user
 * GET /api/activity/recent   - Get recent global activity
 */
public class ActivityServlet extends HttpServlet {
    
    private final ActivityDAO activityDAO;

    public ActivityServlet() {
        this.activityDAO = new ActivityDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Endpoint requires path: /feed, /user/{id}, or /recent");
            return;
        }

        if (pathInfo.equals("/feed")) {
            // GET /api/activity/feed
            handleGetFeed(req, resp);
        } else if (pathInfo.equals("/recent")) {
            // GET /api/activity/recent
            handleGetRecent(req, resp);
        } else if (pathInfo.startsWith("/user/")) {
            // GET /api/activity/user/{id}
            handleGetUserActivity(req, resp, pathInfo);
        } else {
            JsonUtil.writeNotFound(resp, "Endpoint not found");
        }
    }

    /**
     * GET /api/activity/feed - Personalized feed from followed users
     */
    private void handleGetFeed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer currentUserId = (Integer) req.getAttribute("userId");
        
        if (currentUserId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required for personalized feed");
            return;
        }

        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        List<Activity> activities = activityDAO.getFeed(currentUserId, page, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("activities", activities);
        result.put("page", page);
        result.put("limit", limit);

        JsonUtil.writeSuccess(resp, result);
    }

    /**
     * GET /api/activity/user/{id} - Activity for specific user
     */
    private void handleGetUserActivity(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int userId = Integer.parseInt(pathInfo.substring(6)); // "/user/".length() = 6

            int page = getIntParam(req, "page", 1);
            int limit = getIntParam(req, "limit", 20);

            List<Activity> activities = activityDAO.getUserActivity(userId, page, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("activities", activities);
            result.put("userId", userId);
            result.put("page", page);
            result.put("limit", limit);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * GET /api/activity/recent - Recent global activity (discovery)
     */
    private void handleGetRecent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        List<Activity> activities = activityDAO.getRecentActivity(page, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("activities", activities);
        result.put("page", page);
        result.put("limit", limit);

        JsonUtil.writeSuccess(resp, result);
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
}
