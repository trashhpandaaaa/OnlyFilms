package com.onlyfilms.model;

import java.sql.Timestamp;

/**
 * WatchlistItem entity - movies user wants to watch
 */
public class WatchlistItem {
    private int userId;
    private int movieId;
    private Timestamp addedAt;

    // Movie details for display
    private String movieTitle;
    private String moviePosterUrl;
    private Integer movieReleaseYear;

    // Default constructor
    public WatchlistItem() {}

    // Constructor
    public WatchlistItem(int userId, int movieId) {
        this.userId = userId;
        this.movieId = movieId;
    }

    // Getters and Setters
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

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
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
