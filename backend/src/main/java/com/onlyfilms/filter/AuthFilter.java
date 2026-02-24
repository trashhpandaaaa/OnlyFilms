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

        // Parse token eagerly so public endpoints can still receive profile context.
        String authHeader = httpRequest.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Claims claims = null;
        if (token != null) {
            claims = JwtUtil.validateToken(token);
            if (claims != null) {
                httpRequest.setAttribute("userId", Integer.parseInt(claims.getSubject()));
                Number profileIdNum = claims.get("profileId", Number.class);
                httpRequest.setAttribute("profileId", profileIdNum != null ? profileIdNum.intValue() : null);
                httpRequest.setAttribute("displayName", claims.get("displayName", String.class));
            }
        }

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

        if (token == null) {
            JsonUtil.writeUnauthorized(httpResponse, "No token provided");
            return;
        }

        if (claims == null) {
            JsonUtil.writeUnauthorized(httpResponse, "Invalid or expired token");
            return;
        }

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

        // GET requests to movies, genres, activities, and public lists are public
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/movies") || path.startsWith("/api/genres")) {
                return true;
            }
            // Activities are publicly readable
            if (path.startsWith("/api/activity")) {
                // But /api/activity/feed requires auth
                if (!path.equals("/api/activity/feed")) {
                    return true;
                }
            }
            // Public lists can be viewed without auth
            if (path.startsWith("/api/lists")) {
                return true;
            }
            // User profiles are public (except /me)
            if (path.startsWith("/api/users") && !path.equals("/api/users/me")) {
                return true;
            }
            // Favorites are publicly readable
            if (path.startsWith("/api/favorites")) {
                return true;
            }
            // Comments are publicly readable
            if (path.startsWith("/api/comments")) {
                return true;
            }
            // Follows are publicly readable
            if (path.startsWith("/api/follows")) {
                return true;
            }
            // Likes are publicly readable
            if (path.startsWith("/api/likes")) {
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
