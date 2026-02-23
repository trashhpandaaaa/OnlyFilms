package com.onlyfilms.service;

import com.onlyfilms.dao.*;
import com.onlyfilms.model.User;
import com.onlyfilms.model.UserProfile;

import java.util.Optional;

/**
 * Service for User Profile operations
 */
public class UserService {
    
    private final UserDAO userDAO;
    private final FollowDAO followDAO;
    private final ReviewDAO reviewDAO;
    private final WatchlistDAO watchlistDAO;
    private final CustomListDAO listDAO;

    public UserService() {
        this.userDAO = new UserDAO();
        this.followDAO = new FollowDAO();
        this.reviewDAO = new ReviewDAO();
        this.watchlistDAO = new WatchlistDAO();
        this.listDAO = new CustomListDAO();
    }

    /**
     * Get user profile by ID with all stats
     */
    public Optional<UserProfile> getProfile(int userId, Integer currentUserId) {
        Optional<User> userOpt = userDAO.findById(userId);
        
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        UserProfile profile = UserProfile.fromUser(user);

        // Load stats
        profile.setReviewCount(reviewDAO.countByUserId(userId));
        profile.setWatchedCount(watchlistDAO.countWatched(userId));
        profile.setWatchlistCount(watchlistDAO.countWatchlist(userId));
        profile.setListCount(listDAO.countByUserId(userId));
        profile.setFollowersCount(followDAO.countFollowers(userId));
        profile.setFollowingCount(followDAO.countFollowing(userId));

        // Check relationship if viewing another user's profile
        if (currentUserId != null && currentUserId != userId) {
            profile.setIsFollowing(followDAO.isFollowing(currentUserId, userId));
            profile.setIsFollowedBy(followDAO.isFollowing(userId, currentUserId));
        }

        // Hide email for other users
        if (currentUserId == null || currentUserId != userId) {
            profile.setEmail(null);
        }

        return Optional.of(profile);
    }

    /**
     * Get user profile by username
     */
    public Optional<UserProfile> getProfileByUsername(String username, Integer currentUserId) {
        Optional<User> userOpt = userDAO.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        return getProfile(userOpt.get().getId(), currentUserId);
    }

    /**
     * Update user profile
     */
    public boolean updateProfile(int userId, String bio, String avatarUrl) {
        Optional<User> userOpt = userDAO.findById(userId);
        
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setBio(bio);
        user.setAvatarUrl(avatarUrl);
        
        return userDAO.update(user);
    }
}
