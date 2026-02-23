package com.onlyfilms.filter;

import com.onlyfilms.util.JsonUtil;
import com.onlyfilms.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to validate JWT tokens on protected routes
 */
public class AuthFilter implements Filter {

    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/register",
        "/api/auth/login",
        "/api/health",
        "/api/movies",
        "/api/genres",
        "/api/tmdb"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if path is public
        if (isPublicPath(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        // Get Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);

        if (token == null) {
            JsonUtil.writeUnauthorized(httpResponse, "No token provided");
            return;
        }

        // Validate token
        Claims claims = JwtUtil.validateToken(token);
        
        if (claims == null) {
            JsonUtil.writeUnauthorized(httpResponse, "Invalid or expired token");
            return;
        }

        // Add user info to request attributes for use in servlets
        httpRequest.setAttribute("userId", Integer.parseInt(claims.getSubject()));
        httpRequest.setAttribute("username", claims.get("username", String.class));

        chain.doFilter(request, response);
    }

    /**
     * Check if the path is public (doesn't require authentication)
     */
    private boolean isPublicPath(String path, String method) {
        // Health check is always public
        if (path.equals("/api/health")) {
            return true;
        }

        // Auth endpoints are public
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        
        // TMDB sync endpoints (admin - should be secured in production)
        if (path.startsWith("/api/tmdb/")) {
            return true;
        }

        // GET requests to movies, genres, reviews, and public lists are public
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/movies") || path.startsWith("/api/genres")) {
                return true;
            }
            // Reviews are publicly readable
            if (path.startsWith("/api/reviews")) {
                return true;
            }
            // Public lists can be viewed without auth (checked in servlet)
            if (path.startsWith("/api/lists")) {
                return true;
            }
            // User profiles, followers, following are public (except /me)
            if (path.startsWith("/api/users") && !path.equals("/api/users/me")) {
                return true;
            }
            // Activity feeds: user activity and recent are public, /feed requires auth
            if (path.startsWith("/api/activity/user/") || path.equals("/api/activity/recent")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
