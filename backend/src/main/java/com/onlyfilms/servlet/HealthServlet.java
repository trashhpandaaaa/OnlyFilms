package com.onlyfilms.servlet;

import com.onlyfilms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "OnlyFilms API");
        health.put("version", "1.0.0");
        health.put("timestamp", System.currentTimeMillis());
        
        JsonUtil.sendSuccess(resp, health);
    }
}
