package com.onlyfilms.servlet;

import com.onlyfilms.service.TmdbApiService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Person (Actor/Director) endpoints
 * GET /api/person/{id} - Get person details with filmography
 */
public class PersonServlet extends HttpServlet {
    
    private final TmdbApiService tmdbService;

    public PersonServlet() {
        this.tmdbService = TmdbApiService.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            JsonUtil.writeBadRequest(resp, "Person ID required");
            return;
        }

        try {
            int personId = Integer.parseInt(pathInfo.substring(1));
            Map<String, Object> person = tmdbService.getPersonDetails(personId);
            JsonUtil.writeSuccess(resp, person);
        } catch (NumberFormatException e) {
            JsonUtil.writeBadRequest(resp, "Invalid person ID");
        } catch (Exception e) {
            System.err.println("Error fetching person: " + e.getMessage());
            JsonUtil.writeServerError(resp, "Failed to fetch person details");
        }
    }
}
