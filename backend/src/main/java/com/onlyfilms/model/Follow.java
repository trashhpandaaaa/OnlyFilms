package com.onlyfilms.model;

public class Follow {
    private int followerId;
    private int followingId;

    // Transient display fields
    private String followerDisplayName;
    private String followerProfilePic;
    private String followingDisplayName;
    private String followingProfilePic;

    public Follow() {}

    public Follow(int followerId, int followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public int getFollowerId() { return followerId; }
    public void setFollowerId(int followerId) { this.followerId = followerId; }

    public int getFollowingId() { return followingId; }
    public void setFollowingId(int followingId) { this.followingId = followingId; }

    public String getFollowerDisplayName() { return followerDisplayName; }
    public void setFollowerDisplayName(String followerDisplayName) { this.followerDisplayName = followerDisplayName; }

    public String getFollowerProfilePic() { return followerProfilePic; }
    public void setFollowerProfilePic(String followerProfilePic) { this.followerProfilePic = followerProfilePic; }

    public String getFollowingDisplayName() { return followingDisplayName; }
    public void setFollowingDisplayName(String followingDisplayName) { this.followingDisplayName = followingDisplayName; }

    public String getFollowingProfilePic() { return followingProfilePic; }
    public void setFollowingProfilePic(String followingProfilePic) { this.followingProfilePic = followingProfilePic; }
}
