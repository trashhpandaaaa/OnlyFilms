package com.onlyfilms.servlet;

import com.onlyfilms.service.AuthService;
import com.onlyfilms.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class AuthServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) {
            JsonUtil.writeBadRequest(resp, "Invalid endpoint");
            return;
        }

        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            switch (path) {
                case "/register" -> handleRegister(json, resp);
                case "/login" -> handleLogin(json, resp);
                default -> JsonUtil.writeNotFound(resp, "Endpoint not found");
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeBadRequest(resp, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeServerError(resp, "Internal server error: " + e.getMessage());
        }
    }

    private void handleRegister(JsonObject json, HttpServletResponse resp) throws Exception {
        String email = json.has("email") ? json.get("email").getAsString() : null;
        String password = json.has("password") ? json.get("password").getAsString() : null;
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;

        Map<String, Object> result = authService.register(email, password, displayName);
        JsonUtil.writeCreated(resp, result);
    }

    private void handleLogin(JsonObject json, HttpServletResponse resp) throws Exception {
        String email = json.has("email") ? json.get("email").getAsString() : null;
        String password = json.has("password") ? json.get("password").getAsString() : null;

        Map<String, Object> result = authService.login(email, password);
        JsonUtil.writeSuccess(resp, result);
    }
}
