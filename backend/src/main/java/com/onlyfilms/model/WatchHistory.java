package com.onlyfilms.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * WatchHistory entity - log of movies user has watched
 */
public class WatchHistory {
    private int id;
    private int userId;
    private int movieId;
    private Date watchedAt;
    private boolean isRewatch;
    private Timestamp createdAt;

    // Movie details for display
    private String movieTitle;
    private String moviePosterUrl;
    private Integer movieReleaseYear;

    // Default constructor
    public WatchHistory() {}

    // Constructor
    public WatchHistory(int userId, int movieId, Date watchedAt, boolean isRewatch) {
        this.userId = userId;
        this.movieId = movieId;
        this.watchedAt = watchedAt;
        this.isRewatch = isRewatch;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public Date getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(Date watchedAt) {
        this.watchedAt = watchedAt;
    }

    public boolean isRewatch() {
        return isRewatch;
    }

    public void setRewatch(boolean rewatch) {
        isRewatch = rewatch;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMoviePosterUrl() {
        return moviePosterUrl;
    }

    public void setMoviePosterUrl(String moviePosterUrl) {
        this.moviePosterUrl = moviePosterUrl;
    }

    public Integer getMovieReleaseYear() {
        return movieReleaseYear;
    }

    public void setMovieReleaseYear(Integer movieReleaseYear) {
        this.movieReleaseYear = movieReleaseYear;
    }
}
