package com.onlyfilms.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onlyfilms.dao.GenreDAO;
import com.onlyfilms.dao.MovieDAO;
import com.onlyfilms.model.Genre;
import com.onlyfilms.model.Movie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TmdbService {
    // Bearer token for TMDB API
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3YzdlZDA1Y2E5M2I4NWZlMTI4NzkyNWYyYjhkZDM5OSIsIm5iZiI6MTc2ODIxNTI5Ni4xOTcsInN1YiI6IjY5NjRkMzAwMzRhNDFlN2ZlMTI4NDhjNCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.LHfngY2sXBqJxrKP5B2uE4xTspeJJVD7SoGBqyWQTJQ";
    
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280";

    private final MovieDAO movieDAO;
    private final GenreDAO genreDAO;

    public TmdbService() {
        this.movieDAO = new MovieDAO();
        this.genreDAO = new GenreDAO();
    }

    // Fetch and save genres from TMDB
    public int syncGenres() throws Exception {
        String url = BASE_URL + "/genre/movie/list?language=en-US";
        String response = makeRequest(url);
        
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray genres = json.getAsJsonArray("genres");
        int added = 0;

        for (JsonElement element : genres) {
            JsonObject genreJson = element.getAsJsonObject();
            String name = genreJson.get("name").getAsString();
            
            try {
                if (genreDAO.findByName(name) == null) {
                    Genre genre = new Genre();
                    genre.setName(name);
                    genre.setSlug(name.toLowerCase().replace(" ", "-"));
                    genreDAO.save(genre);
                    System.out.println("Added genre: " + name);
                    added++;
                }
            } catch (SQLException e) {
                System.err.println("Error saving genre: " + name + " - " + e.getMessage());
            }
        }
        return added;
    }

    // Fetch popular movies from TMDB
    public List<Movie> fetchPopularMovies(int pages) throws Exception {
        List<Movie> movies = new ArrayList<>();

        for (int page = 1; page <= pages; page++) {
            String url = BASE_URL + "/movie/popular?language=en-US&page=" + page;
            String response = makeRequest(url);
            
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");

            for (JsonElement element : results) {
                Movie movie = parseMovie(element.getAsJsonObject());
                if (movie != null) {
                    movies.add(movie);
                }
            }
            
            System.out.println("Fetched popular page " + page + " - Total: " + movies.size());
            Thread.sleep(250); // Rate limiting
        }

        return movies;
    }

    // Fetch top rated movies from TMDB
    public List<Movie> fetchTopRatedMovies(int pages) throws Exception {
        List<Movie> movies = new ArrayList<>();

        for (int page = 1; page <= pages; page++) {
            String url = BASE_URL + "/movie/top_rated?language=en-US&page=" + page;
            String response = makeRequest(url);
            
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");

            for (JsonElement element : results) {
                Movie movie = parseMovie(element.getAsJsonObject());
                if (movie != null) {
                    movies.add(movie);
                }
            }
            
            System.out.println("Fetched top rated page " + page + " - Total: " + movies.size());
            Thread.sleep(250);
        }

        return movies;
    }

    // Save movies to database
    public int saveMoviesToDatabase(List<Movie> movies) {
        int saved = 0;
        for (Movie movie : movies) {
            try {
                if (!movieDAO.existsByTitle(movie.getTitle())) {
                    movieDAO.save(movie);
                    saved++;
                    System.out.println("Saved: " + movie.getTitle());
                } else {
                    System.out.println("Skipped (exists): " + movie.getTitle());
                }
            } catch (SQLException e) {
                System.err.println("Error saving movie: " + movie.getTitle() + " - " + e.getMessage());
            }
        }
        return saved;
    }

    // Parse movie from TMDB response
    private Movie parseMovie(JsonObject json) {
        try {
            Movie movie = new Movie();
            
            String title = getStringOrNull(json, "title");
            if (title == null || title.isEmpty()) {
                return null;
            }
            movie.setTitle(title);
            movie.setOriginalTitle(getStringOrNull(json, "original_title"));
            movie.setOverview(getStringOrNull(json, "overview"));
            
            String releaseDate = getStringOrNull(json, "release_date");
            if (releaseDate != null && releaseDate.length() >= 10) {
                try {
                    movie.setReleaseDate(LocalDate.parse(releaseDate));
                } catch (Exception e) {
                    // Invalid date format, skip
                }
            }
            
            String posterPath = getStringOrNull(json, "poster_path");
            if (posterPath != null) {
                movie.setPosterUrl(IMAGE_BASE_URL + posterPath);
            }
            
            String backdropPath = getStringOrNull(json, "backdrop_path");
            if (backdropPath != null) {
                movie.setBackdropUrl(BACKDROP_BASE_URL + backdropPath);
            }

            // Language
            movie.setLanguage(getStringOrNull(json, "original_language"));

            // TMDB vote average is 0-10, we'll use it as initial rating
            if (json.has("vote_average") && !json.get("vote_average").isJsonNull()) {
                double voteAvg = json.get("vote_average").getAsDouble();
                movie.setAverageRating(voteAvg / 2.0); // Convert to 0-5 scale
            }

            if (json.has("vote_count") && !json.get("vote_count").isJsonNull()) {
                movie.setRatingCount(json.get("vote_count").getAsInt());
            }

            return movie;
        } catch (Exception e) {
            System.err.println("Error parsing movie: " + e.getMessage());
            return null;
        }
    }

    // Make HTTP request with Bearer token
    private String makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new RuntimeException("HTTP error " + responseCode + ": " + errorResponse);
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
}
