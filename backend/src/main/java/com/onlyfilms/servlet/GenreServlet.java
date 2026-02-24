package com.onlyfilms.servlet;

import com.onlyfilms.service.TmdbApiService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Serves genre data from TMDB.
 * GET /api/genres - list all genres
 */
public class GenreServlet extends HttpServlet {
    private final TmdbApiService tmdbService = TmdbApiService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Map<String, Object>> genres = tmdbService.getGenres();
            JsonUtil.writeSuccess(resp, genres);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error fetching genres: " + e.getMessage());
        }
    }
}
