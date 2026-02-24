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
 * Proxy servlet for TMDB API - handles movie browsing.
 * GET /api/movies/popular
 * GET /api/movies/top-rated
 * GET /api/movies/now-playing
 * GET /api/movies/upcoming
 * GET /api/movies/trending/{timeWindow}
 * GET /api/movies/search?q=query
 * GET /api/movies/{id}
 * GET /api/movies/genre/{genreId}
 */
public class MovieServlet extends HttpServlet {
    private final TmdbApiService tmdbService = TmdbApiService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        int page = getIntParam(req, "page", 1);

        try {
            if (path == null || path.equals("/") || path.equals("/popular")) {
                JsonUtil.writeSuccess(resp, tmdbService.getPopularMovies(page));
            } else if (path.equals("/top-rated")) {
                JsonUtil.writeSuccess(resp, tmdbService.getTopRatedMovies(page));
            } else if (path.equals("/now-playing")) {
                JsonUtil.writeSuccess(resp, tmdbService.getNowPlayingMovies(page));
            } else if (path.equals("/upcoming")) {
                JsonUtil.writeSuccess(resp, tmdbService.getUpcomingMovies(page));
            } else if (path.equals("/search")) {
                String query = req.getParameter("q");
                if (query == null || query.trim().isEmpty()) {
                    JsonUtil.writeBadRequest(resp, "Search query required");
                    return;
                }
                JsonUtil.writeSuccess(resp, tmdbService.searchMovies(query, page));
            } else if (path.startsWith("/trending/")) {
                String timeWindow = path.substring(10);
                JsonUtil.writeSuccess(resp, tmdbService.getTrendingMovies(timeWindow, page));
            } else if (path.startsWith("/genre/")) {
                int genreId = Integer.parseInt(path.substring(7));
                JsonUtil.writeSuccess(resp, tmdbService.getMoviesByGenre(genreId, page));
            } else {
                // Movie details by TMDB ID
                int movieId = Integer.parseInt(path.substring(1));
                JsonUtil.writeSuccess(resp, tmdbService.getMovieDetails(movieId));
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid movie ID");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error fetching movie data: " + e.getMessage());
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
