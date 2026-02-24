package com.onlyfilms.servlet;

import com.onlyfilms.dao.CommentDAO;
import com.onlyfilms.dao.DateDimDAO;
import com.onlyfilms.model.Comment;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Handles comment endpoints:
 * GET    /api/comments/activity/{id} - get comments for an activity
 * POST   /api/comments               - create a comment (auth required)
 * DELETE  /api/comments/{id}          - delete a comment (auth required)
 */
public class CommentServlet extends HttpServlet {
    private final CommentDAO commentDAO = new CommentDAO();
    private final DateDimDAO dateDimDAO = new DateDimDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path != null && path.startsWith("/activity/")) {
                int activityId = Integer.parseInt(path.substring(10));
                List<Comment> comments = commentDAO.findByActivityId(activityId);
                JsonUtil.writeSuccess(resp, comments);
            } else {
                JsonUtil.writeBadRequest(resp, "Use /api/comments/activity/{activityId}");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid ID");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
            String content = json.get("commentContent").getAsString();

            if (content == null || content.trim().isEmpty()) {
                JsonUtil.writeBadRequest(resp, "Comment content is required");
                return;
            }

            int dateId = dateDimDAO.getOrCreateToday();

            Comment comment = new Comment();
            comment.setActivityId(activityId);
            comment.setProfileId(profileId);
            comment.setCommentContent(content.trim());
            comment.setCreatedId(dateId);

            comment = commentDAO.create(comment);
            Comment full = commentDAO.findById(comment.getCommentId());
            JsonUtil.writeCreated(resp, full);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Comment ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int commentId = Integer.parseInt(path.substring(1));
            commentDAO.delete(commentId, profileId);
            JsonUtil.writeSuccess(resp, "Comment deleted", null);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
