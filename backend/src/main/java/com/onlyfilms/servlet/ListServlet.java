package com.onlyfilms.servlet;

import com.onlyfilms.dao.CustomListDAO;
import com.onlyfilms.model.CustomList;
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
 * Custom List endpoints
 * GET    /api/lists              - Get user's lists (auth) or public lists
 * GET    /api/lists/{id}         - Get list by ID
 * POST   /api/lists              - Create new list (auth)
 * PUT    /api/lists/{id}         - Update list (auth)
 * DELETE /api/lists/{id}         - Delete list (auth)
 * POST   /api/lists/{id}/movies/{movieId}   - Add movie to list (auth)
 * DELETE /api/lists/{id}/movies/{movieId}   - Remove movie from list (auth)
 */
public class ListServlet extends HttpServlet {
    
    private final CustomListDAO listDAO;

    public ListServlet() {
        this.listDAO = new CustomListDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = (Integer) req.getAttribute("userId");
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/lists
            handleGetLists(req, resp, userId);
        } else {
            // GET /api/lists/{id}
            handleGetById(req, resp, pathInfo, userId);
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
            // POST /api/lists - Create list
            handleCreate(req, resp, userId);
        } else if (pathInfo.contains("/movies/")) {
            // POST /api/lists/{id}/movies/{movieId}
            handleAddMovie(req, resp, pathInfo, userId);
        } else {
            JsonUtil.writeNotFound(resp, "Endpoint not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Check authentication
        Integer userId = (Integer) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeUnauthorized(resp, "Authentication required");
            return;
        }

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "List ID required");
            return;
        }

        handleUpdate(req, resp, pathInfo, userId);
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
            JsonUtil.writeBadRequest(resp, "List ID required");
            return;
        }

        if (pathInfo.contains("/movies/")) {
            // DELETE /api/lists/{id}/movies/{movieId}
            handleRemoveMovie(req, resp, pathInfo, userId);
        } else {
            // DELETE /api/lists/{id}
            handleDelete(req, resp, pathInfo, userId);
        }
    }

    /**
     * GET /api/lists
     */
    private void handleGetLists(HttpServletRequest req, HttpServletResponse resp, Integer userId) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);
        String type = req.getParameter("type"); // "mine" or "public"

        List<CustomList> lists;
        int total;

        if (userId != null && ("mine".equals(type) || type == null)) {
            // Get user's own lists
            lists = listDAO.findByUserId(userId, page, limit);
            total = listDAO.countByUserId(userId);
        } else {
            // Get public lists
            lists = listDAO.findPublicLists(page, limit);
            total = lists.size(); // Approximate
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lists", lists);
        result.put("page", page);
        result.put("limit", limit);
        result.put("total", total);

        JsonUtil.writeSuccess(resp, result);
    }

    /**
     * GET /api/lists/{id}
     */
    private void handleGetById(HttpServletRequest req, HttpServletResponse resp, String pathInfo, Integer userId) throws IOException {
        try {
            // Handle paths like /123 or /123/movies/456
            String idPart = pathInfo.substring(1);
            if (idPart.contains("/")) {
                idPart = idPart.substring(0, idPart.indexOf("/"));
            }
            int listId = Integer.parseInt(idPart);

            // Check access
            if (!listDAO.canAccess(listId, userId)) {
                JsonUtil.writeForbidden(resp, "This list is private");
                return;
            }

            Optional<CustomList> list = listDAO.findById(listId);

            if (list.isPresent()) {
                JsonUtil.writeSuccess(resp, list.get());
            } else {
                JsonUtil.writeNotFound(resp, "List not found");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid list ID");
        }
    }

    /**
     * POST /api/lists
     */
    private void handleCreate(HttpServletRequest req, HttpServletResponse resp, int userId) throws IOException {
        try {
            ListRequest request = JsonUtil.fromRequest(req, ListRequest.class);

            if (request == null || request.name == null || request.name.trim().isEmpty()) {
                JsonUtil.writeBadRequest(resp, "List name is required");
                return;
            }

            CustomList list = new CustomList(userId, request.name.trim(), request.description, request.isPublic);
            list = listDAO.save(list);

            JsonUtil.writeCreated(resp, list);
        } catch (Exception e) {
            System.err.println("Error creating list: " + e.getMessage());
            JsonUtil.writeServerError(resp, "Failed to create list");
        }
    }

    /**
     * PUT /api/lists/{id}
     */
    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int listId = Integer.parseInt(pathInfo.substring(1));

            // Check ownership
            if (!listDAO.isOwner(listId, userId)) {
                JsonUtil.writeForbidden(resp, "You can only edit your own lists");
                return;
            }

            ListRequest request = JsonUtil.fromRequest(req, ListRequest.class);

            if (request == null || request.name == null || request.name.trim().isEmpty()) {
                JsonUtil.writeBadRequest(resp, "List name is required");
                return;
            }

            CustomList list = new CustomList(userId, request.name.trim(), request.description, request.isPublic);
            list.setId(listId);

            if (listDAO.update(list)) {
                JsonUtil.writeSuccess(resp, "List updated", list);
            } else {
                JsonUtil.writeServerError(resp, "Failed to update list");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid list ID");
        }
    }

    /**
     * DELETE /api/lists/{id}
     */
    private void handleDelete(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            int listId = Integer.parseInt(pathInfo.substring(1));

            if (listDAO.delete(listId, userId)) {
                JsonUtil.writeSuccess(resp, "List deleted", null);
            } else {
                JsonUtil.writeNotFound(resp, "List not found or you don't own it");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid list ID");
        }
    }

    /**
     * POST /api/lists/{id}/movies/{movieId}
     */
    private void handleAddMovie(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            // Parse path like /123/movies/456
            String[] parts = pathInfo.substring(1).split("/movies/");
            if (parts.length != 2) {
                JsonUtil.writeBadRequest(resp, "Invalid path");
                return;
            }

            int listId = Integer.parseInt(parts[0]);
            int movieId = Integer.parseInt(parts[1]);

            if (listDAO.addMovieToList(listId, movieId, userId)) {
                JsonUtil.writeCreated(resp, Map.of("message", "Movie added to list", "listId", listId, "movieId", movieId));
            } else {
                JsonUtil.writeBadRequest(resp, "Failed to add movie. Check list ownership and if movie exists.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid ID");
        }
    }

    /**
     * DELETE /api/lists/{id}/movies/{movieId}
     */
    private void handleRemoveMovie(HttpServletRequest req, HttpServletResponse resp, String pathInfo, int userId) throws IOException {
        try {
            // Parse path like /123/movies/456
            String[] parts = pathInfo.substring(1).split("/movies/");
            if (parts.length != 2) {
                JsonUtil.writeBadRequest(resp, "Invalid path");
                return;
            }

            int listId = Integer.parseInt(parts[0]);
            int movieId = Integer.parseInt(parts[1]);

            if (listDAO.removeMovieFromList(listId, movieId, userId)) {
                JsonUtil.writeSuccess(resp, "Movie removed from list", null);
            } else {
                JsonUtil.writeNotFound(resp, "Movie not in list or you don't own the list");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid ID");
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
    private static class ListRequest {
        String name;
        String description;
        boolean isPublic = true;
    }
}
