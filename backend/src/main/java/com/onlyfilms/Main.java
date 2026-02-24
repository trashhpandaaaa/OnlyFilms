package com.onlyfilms;

import com.onlyfilms.config.DatabaseConfig;
import com.onlyfilms.filter.AuthFilter;
import com.onlyfilms.filter.CorsFilter;
import com.onlyfilms.servlet.ActivityServlet;
import com.onlyfilms.servlet.AuthServlet;
import com.onlyfilms.servlet.CommentServlet;
import com.onlyfilms.servlet.FavoriteFilmServlet;
import com.onlyfilms.servlet.FollowServlet;
import com.onlyfilms.servlet.GenreServlet;
import com.onlyfilms.servlet.HealthServlet;
import com.onlyfilms.servlet.LikeServlet;
import com.onlyfilms.servlet.ListServlet;
import com.onlyfilms.servlet.MovieServlet;
import com.onlyfilms.servlet.UserServlet;
import com.onlyfilms.servlet.TmdbServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * OnlyFilms Backend - Main Entry Point
 * Starts embedded Jetty server with REST API endpoints
 */
public class Main {
    private static final int PORT = 8080;
    private static Server server;

    public static void main(String[] args) {
        try {
            // Initialize database connection pool
            System.out.println("Initializing database connection...");
            DatabaseConfig.initialize();

            // Create and configure Jetty server
            server = new Server(PORT);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // Add CORS filter for all requests
            FilterHolder corsFilter = new FilterHolder(new CorsFilter());
            context.addFilter(corsFilter, "/*", EnumSet.of(DispatcherType.REQUEST));

            // Add Auth filter for protected routes
            FilterHolder authFilter = new FilterHolder(new AuthFilter());
            context.addFilter(authFilter, "/api/*", EnumSet.of(DispatcherType.REQUEST));

            // Register servlets
            registerServlets(context);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                try {
                    if (server != null) {
                        server.stop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            // Start server
            server.start();
            System.out.println("========================================");
            System.out.println("  OnlyFilms Backend Started!");
            System.out.println("  Server running on http://localhost:" + PORT);
            System.out.println("  Health check: http://localhost:" + PORT + "/api/health");
            System.out.println("========================================");
            server.join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void registerServlets(ServletContextHandler context) {
        // Health check endpoint
        context.addServlet(new ServletHolder(new HealthServlet()), "/api/health");

        // Auth endpoints (register, login)
        context.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/*");

        // Movie endpoints (TMDB proxy)
        context.addServlet(new ServletHolder(new MovieServlet()), "/api/movies/*");

        // Genre endpoints (TMDB proxy)
        context.addServlet(new ServletHolder(new GenreServlet()), "/api/genres/*");

        // Activity endpoints (reviews + watch logs)
        context.addServlet(new ServletHolder(new ActivityServlet()), "/api/activity/*");

        // Comment endpoints
        context.addServlet(new ServletHolder(new CommentServlet()), "/api/comments/*");

        // Like endpoints
        context.addServlet(new ServletHolder(new LikeServlet()), "/api/likes/*");

        // Favorite film endpoints
        context.addServlet(new ServletHolder(new FavoriteFilmServlet()), "/api/favorites/*");

        // List endpoints
        context.addServlet(new ServletHolder(new ListServlet()), "/api/lists/*");

        // User Profile endpoints
        context.addServlet(new ServletHolder(new UserServlet()), "/api/users/*");

        // Follow endpoints
        context.addServlet(new ServletHolder(new FollowServlet()), "/api/follows/*");

        // TMDB proxy endpoints (admin)
        context.addServlet(new ServletHolder(new TmdbServlet()), "/api/tmdb/*");
    }
}
