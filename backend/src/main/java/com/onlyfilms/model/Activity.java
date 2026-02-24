package com.onlyfilms.model;

public class Activity {
    private int activityId;
    private int profileId;
    private int filmId;
    private Integer reviewDateId;
    private Integer watchedDateId;
    private Double rating;
    private String watchedStatus; // "watched", "rewatched", "want_to_watch"
    private String reviewDescription;

    // Transient fields for display
    private String displayName;
    private String profilePic;
    private String filmTitle;
    private String posterUrl;
    private Integer tmdbId;
    private String reviewDate;   // resolved from date_dim
    private String watchedDate;  // resolved from date_dim
    private int likeCount;
    private int commentCount;
    private Boolean likedByCurrentUser;

    public Activity() {}

    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public Integer getReviewDateId() { return reviewDateId; }
    public void setReviewDateId(Integer reviewDateId) { this.reviewDateId = reviewDateId; }

    public Integer getWatchedDateId() { return watchedDateId; }
    public void setWatchedDateId(Integer watchedDateId) { this.watchedDateId = watchedDateId; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getWatchedStatus() { return watchedStatus; }
    public void setWatchedStatus(String watchedStatus) { this.watchedStatus = watchedStatus; }

    public String getReviewDescription() { return reviewDescription; }
    public void setReviewDescription(String reviewDescription) { this.reviewDescription = reviewDescription; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public String getWatchedDate() { return watchedDate; }
    public void setWatchedDate(String watchedDate) { this.watchedDate = watchedDate; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public Boolean getLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(Boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
}
