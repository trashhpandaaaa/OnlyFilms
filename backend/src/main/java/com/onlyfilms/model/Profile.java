package com.onlyfilms.model;

/**
 * Profile entity (matches ERD: profile_id, user_id FK, display_name, bio, favorite_movie, profile_pic, join_date_id FK)
 */
public class Profile {
    private int profileId;
    private int userId;
    private String displayName;
    private String bio;
    private String favoriteMovie;
    private String profilePic;
    private Integer joinDateId;

    // Transient fields for display
    private String email; // from user table
    private String joinDate; // resolved from date_dim

    // Stats
    private int reviewCount;
    private int watchedCount;
    private int favoriteCount;
    private int listCount;
    private int followersCount;
    private int followingCount;

    // Relationship to current viewer
    private Boolean isFollowing;
    private Boolean isFollowedBy;

    public Profile() {}

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getFavoriteMovie() { return favoriteMovie; }
    public void setFavoriteMovie(String favoriteMovie) { this.favoriteMovie = favoriteMovie; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public Integer getJoinDateId() { return joinDateId; }
    public void setJoinDateId(Integer joinDateId) { this.joinDateId = joinDateId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getWatchedCount() { return watchedCount; }
    public void setWatchedCount(int watchedCount) { this.watchedCount = watchedCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public int getListCount() { return listCount; }
    public void setListCount(int listCount) { this.listCount = listCount; }

    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }

    public Boolean getIsFollowedBy() { return isFollowedBy; }
    public void setIsFollowedBy(Boolean isFollowedBy) { this.isFollowedBy = isFollowedBy; }

    /**
     * Create a public view (hides email for non-owners)
     */
    public Profile toPublic() {
        Profile p = new Profile();
        p.setProfileId(this.profileId);
        p.setUserId(this.userId);
        p.setDisplayName(this.displayName);
        p.setBio(this.bio);
        p.setFavoriteMovie(this.favoriteMovie);
        p.setProfilePic(this.profilePic);
        p.setJoinDate(this.joinDate);
        p.setReviewCount(this.reviewCount);
        p.setWatchedCount(this.watchedCount);
        p.setFavoriteCount(this.favoriteCount);
        p.setListCount(this.listCount);
        p.setFollowersCount(this.followersCount);
        p.setFollowingCount(this.followingCount);
        p.setIsFollowing(this.isFollowing);
        p.setIsFollowedBy(this.isFollowedBy);
        return p;
    }
}
