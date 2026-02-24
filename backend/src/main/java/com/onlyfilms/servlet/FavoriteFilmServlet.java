package com.onlyfilms.servlet;

import com.onlyfilms.dao.FavoriteFilmDAO;
import com.onlyfilms.dao.FilmDAO;
import com.onlyfilms.model.FavoriteFilm;
import com.onlyfilms.model.Film;
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
 * Handles favorite film endpoints:
 * GET    /api/favorites/{profileId}   - get user's favorite films
 * POST   /api/favorites               - add film to favorites (auth required)
 * DELETE  /api/favorites/{filmId}      - remove film from favorites (auth required)
 * GET    /api/favorites/check/{filmId} - check if film is favorited (auth required)
 */
public class FavoriteFilmServlet extends HttpServlet {
    private final FavoriteFilmDAO favoriteFilmDAO = new FavoriteFilmDAO();
    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path != null && path.startsWith("/check/")) {
                // Check if current user has this film favorited
                Integer profileId = (Integer) req.getAttribute("profileId");
                if (profileId == null) {
                    JsonUtil.writeUnauthorized(resp, "Not authenticated");
                    return;
                }
                int tmdbId = Integer.parseInt(path.substring(7));
                Film film = filmDAO.findByTmdbId(tmdbId);
                boolean isFav = film != null && favoriteFilmDAO.isFavorite(profileId, film.getFilmId());
                Map<String, Object> result = new HashMap<>();
                result.put("isFavorite", isFav);
                JsonUtil.writeSuccess(resp, result);
            } else if (path != null && !path.equals("/")) {
                // Get favorites for a profile
                int profileId = Integer.parseInt(path.substring(1));
                List<FavoriteFilm> favorites = favoriteFilmDAO.findByProfileId(profileId);
                JsonUtil.writeSuccess(resp, favorites);
            } else {
                JsonUtil.writeBadRequest(resp, "Profile ID required");
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

            int tmdbId = json.get("tmdbId").getAsInt();
            String filmTitle = json.has("filmTitle") ? json.get("filmTitle").getAsString() : "Unknown";
            Integer releaseYear = json.has("releaseYear") && !json.get("releaseYear").isJsonNull()
                ? json.get("releaseYear").getAsInt() : null;
            String posterUrl = json.has("posterUrl") ? json.get("posterUrl").getAsString() : null;

            // Ensure film exists locally
            Film film = filmDAO.findOrCreate(tmdbId, filmTitle, releaseYear, null, 0, posterUrl, null);

            FavoriteFilm fav = favoriteFilmDAO.add(profileId, film.getFilmId());
            JsonUtil.writeCreated(resp, fav);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                JsonUtil.writeBadRequest(resp, "Film already in favorites");
            } else {
                e.printStackTrace();
                JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Film ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int tmdbId = Integer.parseInt(path.substring(1));
            // Lookup local film by TMDB ID
            Film film = filmDAO.findByTmdbId(tmdbId);
            if (film == null) {
                JsonUtil.writeNotFound(resp, "Film not found");
                return;
            }
            favoriteFilmDAO.remove(profileId, film.getFilmId());
            JsonUtil.writeSuccess(resp, "Removed from favorites", null);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
