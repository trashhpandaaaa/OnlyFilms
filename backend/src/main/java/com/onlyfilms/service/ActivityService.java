package com.onlyfilms.service;

import com.onlyfilms.dao.ActivityDAO;
import com.onlyfilms.dao.DateDimDAO;
import com.onlyfilms.dao.FilmDAO;
import com.onlyfilms.model.Activity;
import com.onlyfilms.model.Film;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for activity operations (reviews + watch logs).
 * When creating an activity for a TMDB movie, ensures the film exists locally.
 */
public class ActivityService {
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final FilmDAO filmDAO = new FilmDAO();
    private final DateDimDAO dateDimDAO = new DateDimDAO();

    /**
     * Create or update an activity. If the film doesn't exist locally, creates it from TMDB data.
     */
    public Activity createActivity(int profileId, int tmdbId, String filmTitle,
                                   Integer releaseYear, String posterUrl,
                                   Double rating, String watchedStatus,
                                   String reviewDescription) throws SQLException {
        // Ensure film exists locally
        Film film = filmDAO.findOrCreate(tmdbId, filmTitle, releaseYear, null, 0, posterUrl, null);

        // Create date entries
        int todayId = dateDimDAO.getOrCreateToday();

        Activity activity = new Activity();
        activity.setProfileId(profileId);
        activity.setFilmId(film.getFilmId());
        activity.setRating(rating);
        activity.setWatchedStatus(watchedStatus);
        activity.setReviewDescription(reviewDescription);

        if (watchedStatus != null) {
            activity.setWatchedDateId(todayId);
        }
        if (reviewDescription != null && !reviewDescription.trim().isEmpty()) {
            activity.setReviewDateId(todayId);
        }

        return activityDAO.create(activity);
    }

    public Activity getActivity(int activityId) throws SQLException {
        return activityDAO.findById(activityId);
    }

    public void updateActivity(Activity activity) throws SQLException {
        activityDAO.update(activity);
    }

    public void deleteActivity(int activityId, int profileId) throws SQLException {
        activityDAO.delete(activityId, profileId);
    }

    public List<Activity> getActivitiesForFilm(int filmId, int limit, int offset) throws SQLException {
        return activityDAO.findByFilmId(filmId, limit, offset);
    }

    public List<Activity> getActivitiesForProfile(int profileId, int limit, int offset) throws SQLException {
        return activityDAO.findByProfileId(profileId, limit, offset);
    }

    public List<Activity> getRecentActivity(int limit, int offset) throws SQLException {
        return activityDAO.getRecentActivity(limit, offset);
    }

    public List<Activity> getFeed(int profileId, int limit, int offset) throws SQLException {
        return activityDAO.getFeedForProfile(profileId, limit, offset);
    }

    /**
     * Get average rating for a film from all activities.
     */
    public Double getAverageRating(int filmId) throws SQLException {
        Film film = filmDAO.getFilmWithStats(filmId);
        return film != null ? film.getAverageRating() : null;
    }
}
