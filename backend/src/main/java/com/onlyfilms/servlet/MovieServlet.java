package com.onlyfilms.servlet;

import com.onlyfilms.service.RatingService;
import com.onlyfilms.service.TmdbApiService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Movie Servlet - Proxies requests to TMDB API
 * Fetches movie data directly from TMDB without local database storage
 */
@WebServlet("/api/movies/*")
public class MovieServlet extends HttpServlet {
    private final TmdbApiService tmdbService = TmdbApiService.getInstance();
    private final RatingService ratingService = new RatingService();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleMovieList(req, resp);
            } else if (pathInfo.equals("/popular")) {
                handlePopular(req, resp);
            } else if (pathInfo.equals("/top-rated")) {
                handleTopRated(req, resp);
            } else if (pathInfo.equals("/now-playing")) {
                handleNowPlaying(req, resp);
            } else if (pathInfo.equals("/upcoming")) {
                handleUpcoming(req, resp);
            } else if (pathInfo.equals("/trending")) {
                handleTrending(req, resp);
            } else if (pathInfo.matches("/\\d+/ratings")) {
                // GET /api/movies/{id}/ratings - Get detailed ratings breakdown
                handleMovieRatings(pathInfo, resp);
            } else {
                // Get movie by ID
                handleMovieDetails(pathInfo, resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(resp, 500, "Error fetching movies: " + e.getMessage());
        }
    }

    private void handleMovieList(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        String search = req.getParameter("search");
        String genre = req.getParameter("genre");
        String category = req.getParameter("category");
        String sortBy = req.getParameter("sort");
        Integer year = getIntegerParam(req, "year");
        Integer yearGte = getIntegerParam(req, "yearGte");
        Integer yearLte = getIntegerParam(req, "yearLte");
        Double voteGte = getDoubleParam(req, "ratingGte");
        
        Map<String, Object> result;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search movies
            result = tmdbService.searchMovies(search.trim(), page);
        } else if (sortBy != null || year != null || yearGte != null || yearLte != null || voteGte != null) {
            // Discover with filters
            Integer genreId = genre != null ? Integer.parseInt(genre) : null;
            result = tmdbService.discoverMovies(page, sortBy, year, yearGte, yearLte, voteGte, genreId);
        } else if (genre != null) {
            // Filter by genre
            try {
                int genreId = Integer.parseInt(genre);
                result = tmdbService.getMoviesByGenre(genreId, page);
            } catch (NumberFormatException e) {
                result = tmdbService.getPopularMovies(page);
            }
        } else if (category != null) {
            // Category-based lists
            switch (category) {
                case "top-rated":
                    result = tmdbService.getTopRatedMovies(page);
                    break;
                case "now-playing":
                    result = tmdbService.getNowPlayingMovies(page);
                    break;
                case "upcoming":
                    result = tmdbService.getUpcomingMovies(page);
                    break;
                case "trending":
                    result = tmdbService.getTrendingMovies("week", page);
                    break;
                default:
                    result = tmdbService.getPopularMovies(page);
            }
        } else {
            // Default: popular movies
            result = tmdbService.getPopularMovies(page);
        }
        
        JsonUtil.sendSuccess(resp, result);
    }

    private void handlePopular(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        Map<String, Object> result = tmdbService.getPopularMovies(page);
        JsonUtil.sendSuccess(resp, result);
    }

    private void handleTopRated(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        Map<String, Object> result = tmdbService.getTopRatedMovies(page);
        JsonUtil.sendSuccess(resp, result);
    }

    private void handleNowPlaying(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        Map<String, Object> result = tmdbService.getNowPlayingMovies(page);
        JsonUtil.sendSuccess(resp, result);
    }

    private void handleUpcoming(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        Map<String, Object> result = tmdbService.getUpcomingMovies(page);
        JsonUtil.sendSuccess(resp, result);
    }

    private void handleTrending(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int page = getIntParam(req, "page", 1);
        String timeWindow = req.getParameter("time") != null ? req.getParameter("time") : "week";
        Map<String, Object> result = tmdbService.getTrendingMovies(timeWindow, page);
        JsonUtil.sendSuccess(resp, result);
    }

    private void handleMovieDetails(String pathInfo, HttpServletResponse resp) throws Exception {
        try {
            int movieId = Integer.parseInt(pathInfo.substring(1));
            Map<String, Object> movie = tmdbService.getMovieDetails(movieId);
            
            // Add combined ratings (critic + user)
            Double tmdbRating = movie.get("averageRating") != null ? ((Number) movie.get("averageRating")).doubleValue() : null;
            Integer tmdbCount = movie.get("ratingCount") != null ? ((Number) movie.get("ratingCount")).intValue() : null;
            Map<String, Object> ratings = ratingService.getMovieRatings(movieId, tmdbRating, tmdbCount);
            movie.put("ratings", ratings);
            
            JsonUtil.sendSuccess(resp, movie);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, 400, "Invalid movie ID");
        }
    }
    
    /**
     * GET /api/movies/{id}/ratings - Get detailed ratings breakdown for a movie
     */
    private void handleMovieRatings(String pathInfo, HttpServletResponse resp) throws Exception {
        try {
            // Extract movie ID from path like "/123/ratings"
            String idStr = pathInfo.substring(1, pathInfo.indexOf("/ratings"));
            int movieId = Integer.parseInt(idStr);
            
            // Get TMDB movie data for critic rating
            Map<String, Object> movie = tmdbService.getMovieDetails(movieId);
            Double tmdbRating = movie.get("averageRating") != null ? ((Number) movie.get("averageRating")).doubleValue() : null;
            Integer tmdbCount = movie.get("ratingCount") != null ? ((Number) movie.get("ratingCount")).intValue() : null;
            
            // Get combined ratings
            Map<String, Object> ratings = ratingService.getMovieRatings(movieId, tmdbRating, tmdbCount);
            
            // Also add user rating distribution
            Map<String, Object> userStats = ratingService.getUserRatingStats(movieId);
            ratings.put("userDistribution", userStats.get("distribution"));
            
            JsonUtil.sendSuccess(resp, ratings);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, 400, "Invalid movie ID");
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
    
    private Integer getIntegerParam(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Double getDoubleParam(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
