package com.onlyfilms.servlet;

import com.onlyfilms.service.TmdbApiService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * TMDB proxy for admin/sync operations.
 * GET /api/tmdb/person/{id} - get person details from TMDB
 */
public class TmdbServlet extends HttpServlet {
    private final TmdbApiService tmdbService = TmdbApiService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path != null && path.startsWith("/person/")) {
                int personId = Integer.parseInt(path.substring(8));
                Map<String, Object> person = tmdbService.getPersonDetails(personId);
                JsonUtil.writeSuccess(resp, person);
            } else {
                JsonUtil.writeBadRequest(resp, "Invalid TMDB endpoint");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Error: " + e.getMessage());
        }
    }
}
