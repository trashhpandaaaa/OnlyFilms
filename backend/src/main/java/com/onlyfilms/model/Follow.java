package com.onlyfilms.model;

import java.sql.Timestamp;

/**
 * Follow entity - represents a user following another user
 */
public class Follow {
    private int followerId;
    private int followingId;
    private Timestamp createdAt;

    // User details for display
    private String followerUsername;
    private String followerAvatarUrl;
    private String followingUsername;
    private String followingAvatarUrl;

    // Default constructor
    public Follow() {}

    // Constructor
    public Follow(int followerId, int followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    // Getters and Setters
    public int getFollowerId() {
        return followerId;
    }

    public void setFollowerId(int followerId) {
        this.followerId = followerId;
    }

    public int getFollowingId() {
        return followingId;
    }

    public void setFollowingId(int followingId) {
        this.followingId = followingId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFollowerUsername() {
        return followerUsername;
    }

    public void setFollowerUsername(String followerUsername) {
        this.followerUsername = followerUsername;
    }

    public String getFollowerAvatarUrl() {
        return followerAvatarUrl;
    }

    public void setFollowerAvatarUrl(String followerAvatarUrl) {
        this.followerAvatarUrl = followerAvatarUrl;
    }

    public String getFollowingUsername() {
        return followingUsername;
    }

    public void setFollowingUsername(String followingUsername) {
        this.followingUsername = followingUsername;
    }

    public String getFollowingAvatarUrl() {
        return followingAvatarUrl;
    }

    public void setFollowingAvatarUrl(String followingAvatarUrl) {
        this.followingAvatarUrl = followingAvatarUrl;
    }
}
