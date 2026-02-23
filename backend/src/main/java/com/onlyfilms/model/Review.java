package com.onlyfilms.model;

import java.sql.Timestamp;

/**
 * Review entity representing a user's review of a movie
 */
public class Review {
    private int id;
    private int userId;
    private int movieId;
    private int rating; // 1-10
    private String content;
    private boolean containsSpoilers;
    private int likeCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Additional fields for display (joined from other tables)
    private String username;
    private String userAvatarUrl;
    private String movieTitle;

    // Default constructor
    public Review() {}

    // Constructor for creating new review
    public Review(int userId, int movieId, int rating, String content, boolean containsSpoilers) {
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.content = content;
        this.containsSpoilers = containsSpoilers;
    }

    // Full constructor
    public Review(int id, int userId, int movieId, int rating, String content,
                  boolean containsSpoilers, int likeCount, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.content = content;
        this.containsSpoilers = containsSpoilers;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isContainsSpoilers() {
        return containsSpoilers;
    }

    public void setContainsSpoilers(boolean containsSpoilers) {
        this.containsSpoilers = containsSpoilers;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }
}
