package com.onlyfilms.servlet;

import com.onlyfilms.dao.WatchlistDAO;
import com.onlyfilms.model.WatchHistory;
import com.onlyfilms.model.WatchlistItem;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Watchlist and Watch History endpoints
 * GET    /api/watchlist              - Get user's watchlist
 * POST   /api/watchlist/{movieId}    - Add to watchlist
 * DELETE /api/watchlist/{movieId}    - Remove from watchlist
 * GET    /api/watchlist/check/{id}   - Check if movie in watchlist
 * GET    /api/watchlist/history      - Get watch history
 * POST   /api/watchlist/history      - Log watched movie
 * DELETE /api/watchlist/history/{id} - Delete history entry
 */
public class WatchlistServlet extends HttpServlet {
    
    private final WatchlistDAO watchlistDAO;

    public WatchlistServlet() {
        this.watchlistDAO = new WatchlistDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/watchlist
            handleGetWatchlist(req, resp, userId);
        } else if (pathInfo.equals("/history")) {
            // GET /api/watchlist/history
            handleGetHistory(req, resp, userId);
        } else if (pathInfo.startsWith("/check/")) {
            // GET /api/watchlist/check/{movieId}
            handleCheckWatchlist(req, resp, pathInfo, userId);
        } else {
            JsonUtil.writeNotFound(resp, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Movie ID required");
            return;
        }

        if (pathInfo.equals("/history")) {
            // POST /api/watchlist/history
            handleLogWatch(req, resp, userId);
        } else {
            // POST /api/watchlist/{movieId}
            handleAddToWatchlist(req, resp, pathInfo, userId);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "ID required");
            return;
        }

        if (pathInfo.startsWith("/history/")) {
            // DELETE /api/watchlist/history/{id}
            handleDeleteHistory(req, resp, pathInfo, userId);
        } else {
            // DELETE /api/watchlist/{movieId}
            handleRemoveFromWatchlist(req, resp, pathInfo, userId);
        }
    }

    /**
     * GET /api/watchlist
     */
    private void handleGetWatchlist(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        List<WatchlistItem> items = watchlistDAO.getWatchlist(userId, page, limit);
        int total = watchlistDAO.countWatchlist(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("watchlist", items);
        result.put("page", page);
        result.put("limit", limit);
        result.put("total", total);

        JsonUtil.writeSuccess(resp, result);
    }

    /**
     * POST /api/watchlist/{movieId}
     */
    private void handleAddToWatchlist(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int movieId = Integer.parseInt(pathInfo.substring(1));

            if (watchlistDAO.addToWatchlist(userId, movieId)) {
                JsonUtil.writeCreated(resp, Map.of("message", "Added to watchlist", "movieId", movieId));
            } else {
                JsonUtil.writeBadRequest(resp, "Already in watchlist or movie not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid movie ID");
        }
    }

    /**
     * DELETE /api/watchlist/{movieId}
     */
    private void handleRemoveFromWatchlist(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int movieId = Integer.parseInt(pathInfo.substring(1));

            if (watchlistDAO.removeFromWatchlist(userId, movieId)) {
                JsonUtil.writeSuccess(resp, "Removed from watchlist", null);
            } else {
                JsonUtil.writeNotFound(resp, "Movie not in watchlist");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid movie ID");
        }
    }

    /**
     * GET /api/watchlist/check/{movieId}
     */
    private void handleCheckWatchlist(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int movieId = Integer.parseInt(pathInfo.substring(7)); // "/check/".length() = 7

            boolean inWatchlist = watchlistDAO.isInWatchlist(userId, movieId);
            boolean hasWatched = watchlistDAO.hasWatched(userId, movieId);

            Map<String, Object> result = new HashMap<>();
            result.put("movieId", movieId);
            result.put("inWatchlist", inWatchlist);
            result.put("hasWatched", hasWatched);

            JsonUtil.writeSuccess(resp, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid movie ID");
        }
    }

    /**
     * GET /api/watchlist/history
     */
    private void handleGetHistory(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);

        List<WatchHistory> history = watchlistDAO.getWatchHistory(userId, page, limit);
        int total = watchlistDAO.countWatched(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("page", page);
        result.put("limit", limit);
        result.put("totalWatched", total);

        JsonUtil.writeSuccess(resp, result);
    }

    /**
     * POST /api/watchlist/history
     */
    private void handleLogWatch(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        try {
            WatchRequest request = JsonUtil.fromRequest(req, WatchRequest.class);

            if (request == null || request.movieId == 0) {
                JsonUtil.writeBadRequest(resp, "Movie ID is required");
                return;
            }

            Date watchedAt = request.watchedAt != null 
                ? Date.valueOf(request.watchedAt) 
                : new Date(System.currentTimeMillis());

            WatchHistory history = watchlistDAO.logWatch(userId, request.movieId, watchedAt, request.isRewatch);
            JsonUtil.writeCreated(resp, history);
        } catch (IllegalArgumentException e) {
            JsonUtil.writeBadRequest(resp, "Invalid date format. Use YYYY-MM-DD");
        } catch (Exception e) {
            System.err.println("Error logging watch: " + e.getMessage());
            JsonUtil.writeServerError(resp, "Failed to log watch");
        }
    }

    /**
     * DELETE /api/watchlist/history/{id}
     */
    private void handleDeleteHistory(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int id = Integer.parseInt(pathInfo.substring(9)); // "/history/".length() = 9

            if (watchlistDAO.deleteWatchHistory(id, userId)) {
                JsonUtil.writeSuccess(resp, "History entry deleted", null);
            } else {
                JsonUtil.writeNotFound(resp, "History entry not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid history ID");
        }
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
    private static class WatchRequest {
        int movieId;
        String watchedAt; // YYYY-MM-DD
        boolean isRewatch;
    }
}
