package com.onlyfilms.model;

import java.sql.Timestamp;

/**
 * Activity entity - represents an activity item in the feed
 */
public class Activity {
    
    public enum ActivityType {
        REVIEW,      // User posted a review
        WATCH,       // User watched a movie
        LIST_CREATE, // User created a list
        LIST_ADD,    // User added movie to list
        FOLLOW       // User followed someone
    }

    private ActivityType type;
    private Timestamp timestamp;

    // User who performed the action
    private int userId;
    private String username;
    private String userAvatarUrl;

    // Movie info (for REVIEW, WATCH, LIST_ADD)
    private Integer movieId;
    private String movieTitle;
    private String moviePosterUrl;

    // Review info (for REVIEW)
    private Integer reviewId;
    private Integer rating;
    private String reviewContent;

    // List info (for LIST_CREATE, LIST_ADD)
    private Integer listId;
    private String listName;

    // Target user (for FOLLOW)
    private Integer targetUserId;
    private String targetUsername;

    // Default constructor
    public Activity() {}

    // Getters and Setters
    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
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

    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }

    public Integer getListId() {
        return listId;
    }

    public void setListId(Integer listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
}
