package com.onlyfilms.servlet;

import com.onlyfilms.dao.ActivityDAO;
import com.onlyfilms.dao.FilmDAO;
import com.onlyfilms.dao.LikeDAO;
import com.onlyfilms.service.ActivityService;
import com.onlyfilms.model.Activity;
import com.onlyfilms.model.Film;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Collections;

/**
 * Handles activity endpoints (reviews + watch logs):
 * GET  /api/activity/recent       - recent activity from all users
 * GET  /api/activity/feed         - activity from followed users (auth required)
 * GET  /api/activity/profile/{id} - activity for a specific profile
 * GET  /api/activity/film/{id}    - activity for a specific film (by TMDB ID)
 * GET  /api/activity/{id}         - single activity
 * POST /api/activity              - create activity (auth required)
 * PUT  /api/activity/{id}         - update activity (auth required)
 * DELETE /api/activity/{id}       - delete activity (auth required)
 */
public class ActivityServlet extends HttpServlet {
    private final ActivityService activityService = new ActivityService();
    private final FilmDAO filmDAO = new FilmDAO();
    private final LikeDAO likeDAO = new LikeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        int limit = getIntParam(req, "limit", 20);
        int offset = getIntParam(req, "offset", 0);

        try {
            if (path == null || path.equals("/") || path.equals("/recent")) {
                List<Activity> activities = activityService.getRecentActivity(limit, offset);
                enrichWithUserLikes(req, activities);
                JsonUtil.writeSuccess(resp, activities);
            } else if (path.equals("/feed")) {
                Integer profileId = (Integer) req.getAttribute("profileId");
                if (profileId == null) {
                    JsonUtil.writeUnauthorized(resp, "Not authenticated");
                    return;
                }
                List<Activity> activities = activityService.getFeed(profileId, limit, offset);
                enrichWithUserLikes(req, activities);
                JsonUtil.writeSuccess(resp, activities);
            } else if (path.startsWith("/profile/")) {
                int profileId = Integer.parseInt(path.substring(9));
                List<Activity> activities = activityService.getActivitiesForProfile(profileId, limit, offset);
                enrichWithUserLikes(req, activities);
                JsonUtil.writeSuccess(resp, activities);
            } else if (path.startsWith("/film/")) {
                int tmdbId = Integer.parseInt(path.substring(6));
                // Look up local film by TMDB ID
                Film film = filmDAO.findByTmdbId(tmdbId);
                List<Activity> activities;
                if (film != null) {
                    activities = activityService.getActivitiesForFilm(film.getFilmId(), limit, offset);
                } else {
                    activities = Collections.emptyList();
                }
                enrichWithUserLikes(req, activities);
                JsonUtil.writeSuccess(resp, activities);
            } else {
                int activityId = Integer.parseInt(path.substring(1));
                Activity activity = activityService.getActivity(activityId);
                if (activity == null) {
                    JsonUtil.writeNotFound(resp, "Activity not found");
                    return;
                }
                // Check if current user liked it
                Integer currentProfileId = (Integer) req.getAttribute("profileId");
                if (currentProfileId != null) {
                    activity.setLikedByCurrentUser(likeDAO.isLikedBy(activityId, currentProfileId));
                }
                JsonUtil.writeSuccess(resp, activity);
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid ID");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            int tmdbId = json.get("tmdbId").getAsInt();
            String filmTitle = json.has("filmTitle") ? json.get("filmTitle").getAsString() : "Unknown";
            Integer releaseYear = json.has("releaseYear") && !json.get("releaseYear").isJsonNull()
                ? json.get("releaseYear").getAsInt() : null;
            String posterUrl = json.has("posterUrl") ? json.get("posterUrl").getAsString() : null;
            Double rating = json.has("rating") && !json.get("rating").isJsonNull()
                ? json.get("rating").getAsDouble() : null;
            String watchedStatus = json.has("watchedStatus") ? json.get("watchedStatus").getAsString() : null;
            String reviewDescription = json.has("reviewDescription") ? json.get("reviewDescription").getAsString() : null;

            Activity activity = activityService.createActivity(
                profileId, tmdbId, filmTitle, releaseYear, posterUrl,
                rating, watchedStatus, reviewDescription
            );

            // Fetch the full activity with joins
            Activity full = activityService.getActivity(activity.getActivityId());
            JsonUtil.writeCreated(resp, full);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Activity ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int activityId = Integer.parseInt(path.substring(1));
            Activity existing = activityService.getActivity(activityId);
            if (existing == null) {
                JsonUtil.writeNotFound(resp, "Activity not found");
                return;
            }
            if (existing.getProfileId() != profileId) {
                JsonUtil.writeForbidden(resp, "Cannot edit another user's activity");
                return;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            if (json.has("rating")) {
                existing.setRating(json.get("rating").isJsonNull() ? null : json.get("rating").getAsDouble());
            }
            if (json.has("watchedStatus")) {
                existing.setWatchedStatus(json.get("watchedStatus").getAsString());
            }
            if (json.has("reviewDescription")) {
                existing.setReviewDescription(json.get("reviewDescription").getAsString());
            }

            activityService.updateActivity(existing);
            Activity updated = activityService.getActivity(activityId);
            JsonUtil.writeSuccess(resp, "Activity updated", updated);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid activity ID");
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Activity ID required");
            return;
        }

        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId == null) {
            JsonUtil.writeUnauthorized(resp, "Not authenticated");
            return;
        }

        try {
            int activityId = Integer.parseInt(path.substring(1));
            activityService.deleteActivity(activityId, profileId);
            JsonUtil.writeSuccess(resp, "Activity deleted", null);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }

    private void enrichWithUserLikes(HttpServletRequest req, List<Activity> activities) {
        Integer profileId = (Integer) req.getAttribute("profileId");
        if (profileId != null) {
            for (Activity a : activities) {
                try {
                    a.setLikedByCurrentUser(likeDAO.isLikedBy(a.getActivityId(), profileId));
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String val = req.getParameter(name);
        if (val != null) {
            try { return Integer.parseInt(val); } catch (NumberFormatException e) { }
        }
        return defaultValue;
    }
}
