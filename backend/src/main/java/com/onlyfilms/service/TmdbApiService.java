package com.onlyfilms.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching movie data directly from TMDB API
 * No database storage - acts as a proxy to TMDB
 */
public class TmdbApiService {
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3YzdlZDA1Y2E5M2I4NWZlMTI4NzkyNWYyYjhkZDM5OSIsIm5iZiI6MTc2ODIxNTI5Ni4xOTcsInN1YiI6IjY5NjRkMzAwMzRhNDFlN2ZlMTI4NDhjNCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.LHfngY2sXBqJxrKP5B2uE4xTspeJJVD7SoGBqyWQTJQ";
    
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280";

    private static TmdbApiService instance;
    
    // Simple cache to avoid hitting API too often
    private Map<String, CacheEntry> cache = new HashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    public static synchronized TmdbApiService getInstance() {
        if (instance == null) {
            instance = new TmdbApiService();
        }
        return instance;
    }

    /**
     * Get popular movies
     */
    public Map<String, Object> getPopularMovies(int page) throws Exception {
        String cacheKey = "popular_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/movie/popular?language=en-US&page=" + page;
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Get top rated movies
     */
    public Map<String, Object> getTopRatedMovies(int page) throws Exception {
        String cacheKey = "top_rated_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/movie/top_rated?language=en-US&page=" + page;
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Get now playing movies
     */
    public Map<String, Object> getNowPlayingMovies(int page) throws Exception {
        String cacheKey = "now_playing_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/movie/now_playing?language=en-US&page=" + page;
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Get upcoming movies
     */
    public Map<String, Object> getUpcomingMovies(int page) throws Exception {
        String cacheKey = "upcoming_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/movie/upcoming?language=en-US&page=" + page;
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Search movies by query
     */
    public Map<String, Object> searchMovies(String query, int page) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = BASE_URL + "/search/movie?language=en-US&query=" + encodedQuery + "&page=" + page;
        String response = makeRequest(url);
        return parseMovieListResponse(response);
    }

    /**
     * Get movie details by ID
     */
    public Map<String, Object> getMovieDetails(int movieId) throws Exception {
        String cacheKey = "movie_" + movieId;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/movie/" + movieId + "?language=en-US&append_to_response=credits,videos,similar";
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieDetails(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Get movies by genre
     */
    public Map<String, Object> getMoviesByGenre(int genreId, int page) throws Exception {
        String cacheKey = "genre_" + genreId + "_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/discover/movie?language=en-US&with_genres=" + genreId + "&page=" + page + "&sort_by=popularity.desc";
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    /**
     * Get all genres
     */
    public List<Map<String, Object>> getGenres() throws Exception {
        String cacheKey = "genres";
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<Map<String, Object>>) cached.data.get("genres");
        }

        String url = BASE_URL + "/genre/movie/list?language=en-US";
        String response = makeRequest(url);
        
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray genresArray = json.getAsJsonArray("genres");
        
        List<Map<String, Object>> genres = new ArrayList<>();
        for (JsonElement element : genresArray) {
            JsonObject genreJson = element.getAsJsonObject();
            Map<String, Object> genre = new HashMap<>();
            genre.put("id", genreJson.get("id").getAsInt());
            genre.put("name", genreJson.get("name").getAsString());
            genre.put("slug", genreJson.get("name").getAsString().toLowerCase().replace(" ", "-"));
            genres.add(genre);
        }
        
        Map<String, Object> cacheData = new HashMap<>();
        cacheData.put("genres", genres);
        cache.put(cacheKey, new CacheEntry(cacheData));
        
        return genres;
    }

    /**
     * Get trending movies (day or week)
     */
    public Map<String, Object> getTrendingMovies(String timeWindow, int page) throws Exception {
        String cacheKey = "trending_" + timeWindow + "_" + page;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/trending/movie/" + timeWindow + "?language=en-US&page=" + page;
        String response = makeRequest(url);
        Map<String, Object> result = parseMovieListResponse(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    // ===== Helper Methods =====

    private Map<String, Object> parseMovieListResponse(String response) {
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        List<Map<String, Object>> movies = new ArrayList<>();
        JsonArray results = json.getAsJsonArray("results");
        
        for (JsonElement element : results) {
            Map<String, Object> movie = parseMovieBasic(element.getAsJsonObject());
            if (movie != null) {
                movies.add(movie);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("movies", movies);
        result.put("page", json.get("page").getAsInt());
        result.put("total", json.get("total_results").getAsInt());
        result.put("totalPages", json.get("total_pages").getAsInt());
        
        return result;
    }

    private Map<String, Object> parseMovieBasic(JsonObject json) {
        try {
            Map<String, Object> movie = new HashMap<>();
            
            movie.put("id", json.get("id").getAsInt());
            movie.put("title", getStringOrNull(json, "title"));
            movie.put("originalTitle", getStringOrNull(json, "original_title"));
            movie.put("overview", getStringOrNull(json, "overview"));
            movie.put("releaseDate", getStringOrNull(json, "release_date"));
            movie.put("language", getStringOrNull(json, "original_language"));
            
            // Poster and backdrop URLs
            String posterPath = getStringOrNull(json, "poster_path");
            movie.put("posterUrl", posterPath != null ? IMAGE_BASE_URL + posterPath : null);
            
            String backdropPath = getStringOrNull(json, "backdrop_path");
            movie.put("backdropUrl", backdropPath != null ? BACKDROP_BASE_URL + backdropPath : null);
            
            // Rating (convert from 0-10 to 0-5 scale)
            if (json.has("vote_average") && !json.get("vote_average").isJsonNull()) {
                double voteAvg = json.get("vote_average").getAsDouble();
                movie.put("averageRating", Math.round((voteAvg / 2.0) * 100.0) / 100.0);
            }
            
            if (json.has("vote_count") && !json.get("vote_count").isJsonNull()) {
                movie.put("ratingCount", json.get("vote_count").getAsInt());
            }
            
            // Genre IDs
            if (json.has("genre_ids") && !json.get("genre_ids").isJsonNull()) {
                List<Integer> genreIds = new ArrayList<>();
                for (JsonElement g : json.getAsJsonArray("genre_ids")) {
                    genreIds.add(g.getAsInt());
                }
                movie.put("genreIds", genreIds);
            }
            
            return movie;
        } catch (Exception e) {
            System.err.println("Error parsing movie: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseMovieDetails(String response) {
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        Map<String, Object> movie = new HashMap<>();
        
        movie.put("id", json.get("id").getAsInt());
        movie.put("title", getStringOrNull(json, "title"));
        movie.put("originalTitle", getStringOrNull(json, "original_title"));
        movie.put("overview", getStringOrNull(json, "overview"));
        movie.put("releaseDate", getStringOrNull(json, "release_date"));
        movie.put("language", getStringOrNull(json, "original_language"));
        movie.put("runtime", json.has("runtime") && !json.get("runtime").isJsonNull() ? json.get("runtime").getAsInt() : 0);
        movie.put("tagline", getStringOrNull(json, "tagline"));
        movie.put("status", getStringOrNull(json, "status"));
        movie.put("budget", json.has("budget") ? json.get("budget").getAsLong() : 0);
        movie.put("revenue", json.has("revenue") ? json.get("revenue").getAsLong() : 0);
        
        // Poster and backdrop URLs
        String posterPath = getStringOrNull(json, "poster_path");
        movie.put("posterUrl", posterPath != null ? IMAGE_BASE_URL + posterPath : null);
        
        String backdropPath = getStringOrNull(json, "backdrop_path");
        movie.put("backdropUrl", backdropPath != null ? BACKDROP_BASE_URL + backdropPath : null);
        
        // Rating
        if (json.has("vote_average") && !json.get("vote_average").isJsonNull()) {
            double voteAvg = json.get("vote_average").getAsDouble();
            movie.put("averageRating", Math.round((voteAvg / 2.0) * 100.0) / 100.0);
        }
        
        if (json.has("vote_count") && !json.get("vote_count").isJsonNull()) {
            movie.put("ratingCount", json.get("vote_count").getAsInt());
        }
        
        // Genres (full objects)
        if (json.has("genres") && !json.get("genres").isJsonNull()) {
            List<Map<String, Object>> genres = new ArrayList<>();
            for (JsonElement g : json.getAsJsonArray("genres")) {
                JsonObject genreJson = g.getAsJsonObject();
                Map<String, Object> genre = new HashMap<>();
                genre.put("id", genreJson.get("id").getAsInt());
                genre.put("name", genreJson.get("name").getAsString());
                genres.add(genre);
            }
            movie.put("genres", genres);
        }
        
        // Credits (cast and crew)
        if (json.has("credits") && !json.get("credits").isJsonNull()) {
            JsonObject credits = json.getAsJsonObject("credits");
            
            // Cast (top 10)
            if (credits.has("cast") && !credits.get("cast").isJsonNull()) {
                List<Map<String, Object>> cast = new ArrayList<>();
                JsonArray castArray = credits.getAsJsonArray("cast");
                int limit = Math.min(10, castArray.size());
                for (int i = 0; i < limit; i++) {
                    JsonObject person = castArray.get(i).getAsJsonObject();
                    Map<String, Object> castMember = new HashMap<>();
                    castMember.put("id", person.get("id").getAsInt());
                    castMember.put("name", getStringOrNull(person, "name"));
                    castMember.put("character", getStringOrNull(person, "character"));
                    String profilePath = getStringOrNull(person, "profile_path");
                    castMember.put("profileUrl", profilePath != null ? IMAGE_BASE_URL + profilePath : null);
                    cast.add(castMember);
                }
                movie.put("cast", cast);
            }
            
            // Director
            if (credits.has("crew") && !credits.get("crew").isJsonNull()) {
                for (JsonElement c : credits.getAsJsonArray("crew")) {
                    JsonObject crew = c.getAsJsonObject();
                    if ("Director".equals(getStringOrNull(crew, "job"))) {
                        Map<String, Object> director = new HashMap<>();
                        director.put("id", crew.get("id").getAsInt());
                        director.put("name", getStringOrNull(crew, "name"));
                        movie.put("director", director);
                        break;
                    }
                }
            }
        }
        
        // Videos (trailers)
        if (json.has("videos") && !json.get("videos").isJsonNull()) {
            JsonObject videos = json.getAsJsonObject("videos");
            if (videos.has("results") && !videos.get("results").isJsonNull()) {
                List<Map<String, Object>> trailers = new ArrayList<>();
                for (JsonElement v : videos.getAsJsonArray("results")) {
                    JsonObject video = v.getAsJsonObject();
                    if ("YouTube".equals(getStringOrNull(video, "site")) && 
                        "Trailer".equals(getStringOrNull(video, "type"))) {
                        Map<String, Object> trailer = new HashMap<>();
                        trailer.put("key", getStringOrNull(video, "key"));
                        trailer.put("name", getStringOrNull(video, "name"));
                        trailers.add(trailer);
                    }
                }
                movie.put("trailers", trailers);
            }
        }
        
        // Similar movies
        if (json.has("similar") && !json.get("similar").isJsonNull()) {
            JsonObject similar = json.getAsJsonObject("similar");
            if (similar.has("results") && !similar.get("results").isJsonNull()) {
                List<Map<String, Object>> similarMovies = new ArrayList<>();
                JsonArray results = similar.getAsJsonArray("results");
                int limit = Math.min(6, results.size());
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> similarMovie = parseMovieBasic(results.get(i).getAsJsonObject());
                    if (similarMovie != null) {
                        similarMovies.add(similarMovie);
                    }
                }
                movie.put("similar", similarMovies);
            }
        }
        
        return movie;
    }

    /**
     * Get person details with filmography
     */
    public Map<String, Object> getPersonDetails(int personId) throws Exception {
        String cacheKey = "person_" + personId;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        String url = BASE_URL + "/person/" + personId + "?language=en-US&append_to_response=movie_credits";
        String response = makeRequest(url);
        Map<String, Object> result = parsePersonDetails(response);
        
        cache.put(cacheKey, new CacheEntry(result));
        return result;
    }

    private Map<String, Object> parsePersonDetails(String response) {
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        Map<String, Object> person = new HashMap<>();
        person.put("id", json.get("id").getAsInt());
        person.put("name", getStringOrNull(json, "name"));
        person.put("biography", getStringOrNull(json, "biography"));
        person.put("birthday", getStringOrNull(json, "birthday"));
        person.put("deathday", getStringOrNull(json, "deathday"));
        person.put("placeOfBirth", getStringOrNull(json, "place_of_birth"));
        person.put("knownFor", getStringOrNull(json, "known_for_department"));
        
        String profilePath = getStringOrNull(json, "profile_path");
        person.put("profileUrl", profilePath != null ? IMAGE_BASE_URL + profilePath : null);
        
        // Movie credits
        if (json.has("movie_credits") && !json.get("movie_credits").isJsonNull()) {
            JsonObject credits = json.getAsJsonObject("movie_credits");
            
            // Cast filmography
            if (credits.has("cast") && !credits.get("cast").isJsonNull()) {
                List<Map<String, Object>> castFilms = new ArrayList<>();
                JsonArray castArray = credits.getAsJsonArray("cast");
                for (JsonElement element : castArray) {
                    Map<String, Object> movie = parseMovieBasic(element.getAsJsonObject());
                    if (movie != null) {
                        JsonObject movieJson = element.getAsJsonObject();
                        movie.put("character", getStringOrNull(movieJson, "character"));
                        castFilms.add(movie);
                    }
                }
                // Sort by release date descending
                castFilms.sort((a, b) -> {
                    String dateA = (String) a.get("releaseDate");
                    String dateB = (String) b.get("releaseDate");
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                });
                person.put("castCredits", castFilms);
            }
            
            // Crew filmography (director, writer, etc.)
            if (credits.has("crew") && !credits.get("crew").isJsonNull()) {
                List<Map<String, Object>> crewFilms = new ArrayList<>();
                JsonArray crewArray = credits.getAsJsonArray("crew");
                for (JsonElement element : crewArray) {
                    Map<String, Object> movie = parseMovieBasic(element.getAsJsonObject());
                    if (movie != null) {
                        JsonObject movieJson = element.getAsJsonObject();
                        movie.put("job", getStringOrNull(movieJson, "job"));
                        crewFilms.add(movie);
                    }
                }
                crewFilms.sort((a, b) -> {
                    String dateA = (String) a.get("releaseDate");
                    String dateB = (String) b.get("releaseDate");
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                });
                person.put("crewCredits", crewFilms);
            }
        }
        
        return person;
    }

    /**
     * Discover movies with filters
     */
    public Map<String, Object> discoverMovies(int page, String sortBy, Integer year, Integer yearGte, Integer yearLte, Double voteGte, Integer genreId) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/discover/movie?language=en-US&page=" + page);
        
        if (sortBy != null && !sortBy.isEmpty()) {
            urlBuilder.append("&sort_by=").append(sortBy);
        } else {
            urlBuilder.append("&sort_by=popularity.desc");
        }
        
        if (year != null) {
            urlBuilder.append("&primary_release_year=").append(year);
        }
        if (yearGte != null) {
            urlBuilder.append("&primary_release_date.gte=").append(yearGte).append("-01-01");
        }
        if (yearLte != null) {
            urlBuilder.append("&primary_release_date.lte=").append(yearLte).append("-12-31");
        }
        if (voteGte != null) {
            // Convert from 0-5 scale to 0-10 scale
            urlBuilder.append("&vote_average.gte=").append(voteGte * 2);
        }
        if (genreId != null) {
            urlBuilder.append("&with_genres=").append(genreId);
        }
        
        String response = makeRequest(urlBuilder.toString());
        return parseMovieListResponse(response);
    }

    private String makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new RuntimeException("TMDB API error " + responseCode + ": " + errorResponse);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return response.toString();
    }

    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }

    // Simple cache entry class
    private static class CacheEntry {
        Map<String, Object> data;
        long timestamp;

        CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
}
