package com.onlyfilms.servlet;

import com.onlyfilms.model.Movie;
import com.onlyfilms.service.TmdbService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TmdbServlet extends HttpServlet {
    private final TmdbService tmdbService = new TmdbService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                JsonUtil.sendError(resp, 400, "Specify action: /sync-genres, /sync-popular, /sync-top-rated");
                return;
            }

            switch (pathInfo) {
                case "/sync-genres":
                    syncGenres(resp);
                    break;
                case "/sync-popular":
                    syncPopularMovies(req, resp);
                    break;
                case "/sync-top-rated":
                    syncTopRatedMovies(req, resp);
                    break;
                default:
                    JsonUtil.sendError(resp, 404, "Unknown action: " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, 500, "TMDB sync error: " + e.getMessage());
        }
    }

    private void syncGenres(HttpServletResponse resp) throws Exception {
        int added = tmdbService.syncGenres();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Genres synced successfully");
        result.put("added", added);
        JsonUtil.sendSuccess(resp, result);
    }

    private void syncPopularMovies(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int pages = getIntParam(req, "pages", 5);

        List<Movie> movies = tmdbService.fetchPopularMovies(pages);
        int saved = tmdbService.saveMoviesToDatabase(movies);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Popular movies synced");
        result.put("fetched", movies.size());
        result.put("saved", saved);
        JsonUtil.sendSuccess(resp, result);
    }

    private void syncTopRatedMovies(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int pages = getIntParam(req, "pages", 5);

        List<Movie> movies = tmdbService.fetchTopRatedMovies(pages);
        int saved = tmdbService.saveMoviesToDatabase(movies);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Top rated movies synced");
        result.put("fetched", movies.size());
        result.put("saved", saved);
        JsonUtil.sendSuccess(resp, result);
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
