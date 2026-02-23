package com.onlyfilms.model;

import java.time.LocalDateTime;

/**
 * UserProfile DTO - Extended user info with stats for profile pages
 */
public class UserProfile {
    private int id;
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private LocalDateTime createdAt;

    // Stats
    private int reviewCount;
    private int watchedCount;
    private int watchlistCount;
    private int listCount;
    private int followersCount;
    private int followingCount;

    // Relationship to current user
    private Boolean isFollowing;
    private Boolean isFollowedBy;

    // Default constructor
    public UserProfile() {}

    // From User
    public static UserProfile fromUser(User user) {
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setBio(user.getBio());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setCreatedAt(user.getCreatedAt());
        return profile;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getWatchedCount() {
        return watchedCount;
    }

    public void setWatchedCount(int watchedCount) {
        this.watchedCount = watchedCount;
    }

    public int getWatchlistCount() {
        return watchlistCount;
    }

    public void setWatchlistCount(int watchlistCount) {
        this.watchlistCount = watchlistCount;
    }

    public int getListCount() {
        return listCount;
    }

    public void setListCount(int listCount) {
        this.listCount = listCount;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean following) {
        isFollowing = following;
    }

    public Boolean getIsFollowedBy() {
        return isFollowedBy;
    }

    public void setIsFollowedBy(Boolean followedBy) {
        isFollowedBy = followedBy;
    }
}
