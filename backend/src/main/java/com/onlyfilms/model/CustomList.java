package com.onlyfilms.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomList entity - user-created movie lists
 */
public class CustomList {
    private int id;
    private int userId;
    private String name;
    private String description;
    private boolean isPublic;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // User info for display
    private String username;

    // Movies in the list
    private List<ListItem> items = new ArrayList<>();
    private int movieCount;

    // Default constructor
    public CustomList() {}

    // Constructor
    public CustomList(int userId, String name, String description, boolean isPublic) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
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

    public List<ListItem> getItems() {
        return items;
    }

    public void setItems(List<ListItem> items) {
        this.items = items;
    }

    public int getMovieCount() {
        return movieCount;
    }

    public void setMovieCount(int movieCount) {
        this.movieCount = movieCount;
    }

    /**
     * Inner class for list items
     */
    public static class ListItem {
        private int movieId;
        private int position;
        private Timestamp addedAt;
        
        // Movie details
        private String movieTitle;
        private String moviePosterUrl;
        private Integer movieReleaseYear;

        public ListItem() {}

        public ListItem(int movieId, int position) {
            this.movieId = movieId;
            this.position = position;
        }

        public int getMovieId() {
            return movieId;
        }

        public void setMovieId(int movieId) {
            this.movieId = movieId;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
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
}
