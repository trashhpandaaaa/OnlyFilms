package com.onlyfilms.servlet;

import com.onlyfilms.dao.DateDimDAO;
import com.onlyfilms.dao.FilmDAO;
import com.onlyfilms.dao.FilmListDAO;
import com.onlyfilms.model.Film;
import com.onlyfilms.model.FilmList;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Handles list endpoints:
 * GET    /api/lists                   - public lists
 * GET    /api/lists/{id}              - single list with films
 * GET    /api/lists/profile/{id}      - lists by profile
 * POST   /api/lists                   - create list (auth required)
 * PUT    /api/lists/{id}              - update list (auth required)
 * DELETE  /api/lists/{id}              - delete list (auth required)
 * POST   /api/lists/{id}/films        - add film to list (auth required)
 * DELETE  /api/lists/{id}/films/{fid}  - remove film from list (auth required)
 */
public class ListServlet extends HttpServlet {
    private final FilmListDAO listDAO = new FilmListDAO();
    private final FilmDAO filmDAO = new FilmDAO();
    private final DateDimDAO dateDimDAO = new DateDimDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        int limit = getIntParam(req, "limit", 20);
        int offset = getIntParam(req, "offset", 0);

        try {
            if (path == null || path.equals("/")) {
                // Public lists
                List<FilmList> lists = listDAO.getPublicLists(limit, offset);
                JsonUtil.writeSuccess(resp, lists);
            } else if (path.startsWith("/profile/")) {
                int profileId = Integer.parseInt(path.substring(9));
                List<FilmList> lists = listDAO.findByProfileId(profileId);
                JsonUtil.writeSuccess(resp, lists);
            } else {
                // Parse list ID (might have /films suffix)
                String idPart = path.substring(1);
                if (idPart.contains("/")) {
                    idPart = idPart.substring(0, idPart.indexOf("/"));
                }
                int listId = Integer.parseInt(idPart);
                FilmList list = listDAO.findById(listId);
                if (list == null) {
                    JsonUtil.writeNotFound(resp, "List not found");
                    return;
                }
                // Check if list is public or owned by current user
                Integer profileId = (Integer) req.getAttribute("profileId");
                if (!list.getIsPublic() && (profileId == null || profileId != list.getProfileId())) {
                    JsonUtil.writeForbidden(resp, "This list is private");
                    return;
                }
                JsonUtil.writeSuccess(resp, list);
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
        String path = req.getPathInfo();
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

            // Check if adding film to an existing list
            if (path != null && path.matches("/\\d+/films")) {
                int listId = Integer.parseInt(path.substring(1, path.indexOf("/films")));

                // Verify ownership
                FilmList list = listDAO.findById(listId);
                if (list == null) {
                    JsonUtil.writeNotFound(resp, "List not found");
                    return;
                }
                if (list.getProfileId() != profileId) {
                    JsonUtil.writeForbidden(resp, "Not your list");
                    return;
                }

                if (!json.has("tmdbId") || json.get("tmdbId").isJsonNull()) {
                    JsonUtil.writeBadRequest(resp, "tmdbId is required");
                    return;
                }

                int tmdbId;
                try {
                    tmdbId = Integer.parseInt(json.get("tmdbId").getAsString());
                } catch (Exception e) {
                    JsonUtil.writeBadRequest(resp, "Invalid tmdbId");
                    return;
                }

                String filmTitle = (json.has("filmTitle") && !json.get("filmTitle").isJsonNull())
                    ? json.get("filmTitle").getAsString() : "Unknown";

                Integer releaseYear = null;
                if (json.has("releaseYear") && !json.get("releaseYear").isJsonNull()) {
                    String yearRaw = json.get("releaseYear").getAsString().trim();
                    if (!yearRaw.isEmpty()) {
                        try {
                            releaseYear = Integer.parseInt(yearRaw);
                        } catch (NumberFormatException ignored) {
                            // Keep null if releaseYear is malformed instead of failing the whole request.
                        }
                    }
                }
                String posterUrl = json.has("posterUrl") ? json.get("posterUrl").getAsString() : null;

                Film film = filmDAO.findOrCreate(tmdbId, filmTitle, releaseYear, null, 0, posterUrl, null);
                listDAO.addFilmToList(listId, film.getFilmId());

                FilmList updated = listDAO.findById(listId);
                JsonUtil.writeSuccess(resp, "Film added to list", updated);
                return;
            }

            // Create new list
            String listName = json.get("listName").getAsString();
            String listDesc = json.has("listDescription") ? json.get("listDescription").getAsString() : null;
            boolean isPublic = !json.has("isPublic") || json.get("isPublic").getAsBoolean();

            int dateId = dateDimDAO.getOrCreateToday();

            FilmList newList = new FilmList();
            newList.setProfileId(profileId);
            newList.setListName(listName);
            newList.setListDescription(listDesc);
            newList.setCreatedDateId(dateId);
            newList.setIsPublic(isPublic);

            newList = listDAO.create(newList);
            FilmList full = listDAO.findById(newList.getListId());
            JsonUtil.writeCreated(resp, full);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "List ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int listId = Integer.parseInt(path.substring(1));
            FilmList existing = listDAO.findById(listId);
            if (existing == null) {
                JsonUtil.writeNotFound(resp, "List not found");
                return;
            }
            if (existing.getProfileId() != profileId) {
                JsonUtil.writeForbidden(resp, "Not your list");
                return;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            if (json.has("listName")) existing.setListName(json.get("listName").getAsString());
            if (json.has("listDescription")) existing.setListDescription(json.get("listDescription").getAsString());
            if (json.has("isPublic")) existing.setIsPublic(json.get("isPublic").getAsBoolean());

            listDAO.update(existing);
            FilmList updated = listDAO.findById(listId);
            JsonUtil.writeSuccess(resp, "List updated", updated);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "List ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            // Check if removing a film from a list
            if (path.matches("/\\d+/films/\\d+")) {
                String[] parts = path.substring(1).split("/");
                int listId = Integer.parseInt(parts[0]);
                int tmdbId = Integer.parseInt(parts[2]);

                FilmList list = listDAO.findById(listId);
                if (list == null || list.getProfileId() != profileId) {
                    JsonUtil.writeForbidden(resp, "Not your list");
                    return;
                }

                Film film = filmDAO.findByTmdbId(tmdbId);
                if (film == null) {
                    JsonUtil.writeNotFound(resp, "Film not found");
                    return;
                }

                listDAO.removeFilmFromList(listId, film.getFilmId());
                JsonUtil.writeSuccess(resp, "Film removed from list", null);
                return;
            }

            int listId = Integer.parseInt(path.substring(1));
            listDAO.delete(listId, profileId);
            JsonUtil.writeSuccess(resp, "List deleted", null);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    private int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String val = req.getParameter(name);
        if (val != null) {
            try { return Integer.parseInt(val); } catch (NumberFormatException e) { }
        }
        return defaultValue;
    }
}
