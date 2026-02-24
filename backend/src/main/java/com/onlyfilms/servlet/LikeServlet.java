package com.onlyfilms.servlet;

import com.onlyfilms.dao.LikeDAO;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles like endpoints:
 * POST /api/likes/toggle - toggle like on an activity (auth required)
 * GET  /api/likes/activity/{id} - get likes for an activity
 */
public class LikeServlet extends HttpServlet {
    private final LikeDAO likeDAO = new LikeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path != null && path.startsWith("/activity/")) {
                int activityId = Integer.parseInt(path.substring(10));
                JsonUtil.writeSuccess(resp, likeDAO.findByActivityId(activityId));
            } else {
                JsonUtil.writeBadRequest(resp, "Use /api/likes/activity/{activityId}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (!"/toggle".equals(path)) {
            JsonUtil.writeBadRequest(resp, "Use POST /api/likes/toggle");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            int activityId = json.get("activityId").getAsInt();

            boolean liked = likeDAO.toggle(activityId, profileId);
            int count = likeDAO.countByActivityId(activityId);

            Map<String, Object> result = new HashMap<>();
            result.put("liked", liked);
            result.put("likeCount", count);
            JsonUtil.writeSuccess(resp, result);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
