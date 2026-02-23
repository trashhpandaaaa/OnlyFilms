package com.onlyfilms.servlet;

import com.onlyfilms.dao.ReviewDAO;
import com.onlyfilms.model.Review;
import com.onlyfilms.service.ReviewService;
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
 * Review endpoints
 * GET    /api/reviews/movie/{id}    - Get reviews for a movie
 * GET    /api/reviews/user/{id}     - Get reviews by a user
 * GET    /api/reviews/{id}          - Get single review
 * GET    /api/reviews/recent        - Get recent reviews
 * POST   /api/reviews               - Create review (auth required)
 * PUT    /api/reviews/{id}          - Update review (auth required)
 * DELETE /api/reviews/{id}          - Delete review (auth required)
 * POST   /api/reviews/{id}/like     - Like review (auth required)
 * DELETE /api/reviews/{id}/like     - Unlike review (auth required)
 */
public class ReviewServlet extends HttpServlet {
    
    private final ReviewService reviewService;
    private final ReviewDAO reviewDAO;

    public ReviewServlet() {
        this.reviewService = new ReviewService();
        this.reviewDAO = new ReviewDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Invalid endpoint");
            return;
        }

        if (pathInfo.equals("/recent")) {
            handleGetRecent(req, resp);
        } else if (pathInfo.startsWith("/movie/")) {
            handleGetByMovie(req, resp, pathInfo);
        } else if (pathInfo.startsWith("/user/")) {
            handleGetByUser(req, resp, pathInfo);
        } else {
            handleGetById(req, resp, pathInfo);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        if (pathInfo != null && pathInfo.endsWith("/like")) {
            // POST /api/reviews/{id}/like
            handleLike(req, resp, pathInfo, userId);
        } else {
            // POST /api/reviews - Create review
            handleCreate(req, resp, userId);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Review ID required");
            return;
        }

        handleUpdate(req, resp, pathInfo, userId);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Review ID required");
            return;
        }

        if (pathInfo.endsWith("/like")) {
            // DELETE /api/reviews/{id}/like
            handleUnlike(req, resp, pathInfo, userId);
        } else {
            // DELETE /api/reviews/{id}
            handleDelete(req, resp, pathInfo, userId);
        }
    }

    /**
     * GET /api/reviews/movie/{id}
     */
    private void handleGetByMovie(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int movieId = Integer.parseInt(pathInfo.substring(7)); // "/movie/".length() = 7
            int page = getIntParam(req, "page", 1);
            int limit = getIntParam(req, "limit", 20);

            List<Review> reviews = reviewService.getReviewsByMovie(movieId, page, limit);
            int total = reviewDAO.countByMovieId(movieId);

            Map<String, Object> result = new HashMap<>();
            result.put("reviews", reviews);
            result.put("movieId", movieId);
            result.put("page", page);
            result.put("limit", limit);
            result.put("total", total);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid movie ID");
        }
    }

    /**
     * GET /api/reviews/user/{id}
     */
    private void handleGetByUser(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int userId = Integer.parseInt(pathInfo.substring(6)); // "/user/".length() = 6
            int page = getIntParam(req, "page", 1);
            int limit = getIntParam(req, "limit", 20);

            List<Review> reviews = reviewService.getReviewsByUser(userId, page, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("reviews", reviews);
            result.put("userId", userId);
            result.put("page", page);
            result.put("limit", limit);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid user ID");
        }
    }

    /**
     * GET /api/reviews/{id}
     */
    private void handleGetById(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException {
        try {
            int id = Integer.parseInt(pathInfo.substring(1));
            Optional<Review> review = reviewDAO.findById(id);

            if (review.isPresent()) {
                JsonUtil.writeSuccess(resp, review.get());
            } else {
                JsonUtil.writeNotFound(resp, "Review not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid review ID");
        }
    }

    /**
     * GET /api/reviews/recent
     */
    private void handleGetRecent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int limit = getIntParam(req, "limit", 10);
        List<Review> reviews = reviewDAO.findRecent(limit);
        JsonUtil.writeSuccess(resp, reviews);
    }

    /**
     * POST /api/reviews
     */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        try {
            ReviewRequest request = JsonUtil.fromRequest(req, ReviewRequest.class);
            
            if (request == null || request.movieId == 0) {
                JsonUtil.writeBadRequest(resp, "Movie ID is required");
                return;
            }

            if (request.rating == 0) {
                JsonUtil.writeBadRequest(resp, "Rating is required");
                return;
            }

            ReviewService.ReviewResult result = reviewService.createReview(
                userId,
                request.movieId,
                request.rating,
                request.content,
                request.containsSpoilers
            );

            if (result.isSuccess()) {
                JsonUtil.writeCreated(resp, result.getReview());
            } else {
                JsonUtil.writeBadRequest(resp, result.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error creating review: " + e.getMessage());
            JsonUtil.writeServerError(resp, "Failed to create review");
        }
    }

    /**
     * PUT /api/reviews/{id}
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int reviewId = Integer.parseInt(pathInfo.substring(1));
            ReviewRequest request = JsonUtil.fromRequest(req, ReviewRequest.class);
            
            if (request == null || request.rating == 0) {
                JsonUtil.writeBadRequest(resp, "Rating is required");
                return;
            }

            ReviewService.ReviewResult result = reviewService.updateReview(
                reviewId,
                userId,
                request.rating,
                request.content,
                request.containsSpoilers
            );

            if (result.isSuccess()) {
                JsonUtil.writeSuccess(resp, result.getReview());
            } else {
                JsonUtil.writeBadRequest(resp, result.getMessage());
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid review ID");
        } catch (Exception e) {
            System.err.println("Error updating review: " + e.getMessage());
            JsonUtil.writeServerError(resp, "Failed to update review");
        }
    }

    /**
     * DELETE /api/reviews/{id}
     */
    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int reviewId = Integer.parseInt(pathInfo.substring(1));

            ReviewService.ReviewResult result = reviewService.deleteReview(reviewId, userId);

            if (result.isSuccess()) {
                JsonUtil.writeSuccess(resp, "Review deleted", null);
            } else {
                JsonUtil.writeBadRequest(resp, result.getMessage());
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid review ID");
        }
    }

    /**
     * POST /api/reviews/{id}/like
     */
    private void handleLike(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            // Extract review ID from path like "/123/like"
            String idPart = pathInfo.substring(1, pathInfo.length() - 5); // Remove leading "/" and trailing "/like"
            int reviewId = Integer.parseInt(idPart);

            if (reviewService.likeReview(userId, reviewId)) {
                JsonUtil.writeSuccess(resp, "Review liked", null);
            } else {
                JsonUtil.writeBadRequest(resp, "Already liked or review not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid review ID");
        }
    }

    /**
     * DELETE /api/reviews/{id}/like
     */
    private void handleUnlike(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            // Extract review ID from path like "/123/like"
            String idPart = pathInfo.substring(1, pathInfo.length() - 5); // Remove leading "/" and trailing "/like"
            int reviewId = Integer.parseInt(idPart);

            if (reviewService.unlikeReview(userId, reviewId)) {
                JsonUtil.writeSuccess(resp, "Review unliked", null);
            } else {
                JsonUtil.writeBadRequest(resp, "Not liked or review not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid review ID");
        }
    }

    /**
     * Helper to get integer parameter with default value
     */
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
    private static class ReviewRequest {
        int movieId;
        int rating;
        String content;
        boolean containsSpoilers;
    }
}
