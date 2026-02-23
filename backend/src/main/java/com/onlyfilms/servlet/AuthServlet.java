package com.onlyfilms.servlet;

import com.onlyfilms.service.AuthService;
import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private final AuthService authService = new AuthService();
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null) {
            JsonUtil.sendError(resp, 400, "Invalid endpoint");
            return;
        }
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = JsonUtil.fromJson(sb.toString(), Map.class);
        
        switch (pathInfo) {
            case "/register" -> handleRegister(body, resp);
            case "/login" -> handleLogin(body, resp);
            default -> JsonUtil.sendError(resp, 404, "Endpoint not found");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        
        if ("/me".equals(pathInfo)) {
            Integer userId = (Integer) req.getAttribute("userId");
            if (userId == null) {
                JsonUtil.sendError(resp, 401, "Unauthorized");
                return;
            }
            
            authService.getCurrentUser(userId).ifPresentOrElse(
                user -> {
                    try {
                        JsonUtil.sendSuccess(resp, user.toPublicUser());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                },
                () -> {
                    try {
                        JsonUtil.sendError(resp, 404, "User not found");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            );
        } else {
            JsonUtil.sendError(resp, 404, "Endpoint not found");
        }
    }
    
    private void handleRegister(Map<String, String> body, HttpServletResponse resp) throws IOException {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        
        Map<String, Object> result = authService.register(username, email, password);
        
        if ((boolean) result.get("success")) {
            JsonUtil.sendJson(resp, 201, result);
        } else {
            JsonUtil.sendJson(resp, 400, result);
        }
    }
    
    private void handleLogin(Map<String, String> body, HttpServletResponse resp) throws IOException {
        String username = body.get("username");
        String password = body.get("password");
        
        Map<String, Object> result = authService.login(username, password);
        
        if ((boolean) result.get("success")) {
            JsonUtil.sendJson(resp, 200, result);
        } else {
            JsonUtil.sendJson(resp, 401, result);
        }
    }
}
