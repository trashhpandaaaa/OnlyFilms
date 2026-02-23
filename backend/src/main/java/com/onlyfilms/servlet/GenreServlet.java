package com.onlyfilms.servlet;

import com.onlyfilms.service.TmdbApiService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Genre Servlet - Fetches genres directly from TMDB API
 */
@WebServlet("/api/genres/*")
public class GenreServlet extends HttpServlet {
    private final TmdbApiService tmdbService = TmdbApiService.getInstance();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all genres
                List<Map<String, Object>> genres = tmdbService.getGenres();
                JsonUtil.sendSuccess(resp, genres);
            } else {
                // Get genre by ID - return movies for that genre
                try {
                    int genreId = Integer.parseInt(pathInfo.substring(1));
                    int page = getIntParam(req, "page", 1);
                    Map<String, Object> movies = tmdbService.getMoviesByGenre(genreId, page);
                    JsonUtil.sendSuccess(resp, movies);
                } catch (NumberFormatException e) {
                    JsonUtil.sendError(resp, 400, "Invalid genre ID");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, 500, "Error fetching genres: " + e.getMessage());
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
}
