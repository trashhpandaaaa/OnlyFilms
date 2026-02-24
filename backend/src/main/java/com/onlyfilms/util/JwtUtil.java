package com.onlyfilms.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    // In production, use environment variable or secure config
    private static final String SECRET = "OnlyFilmsSecretKeyForJWTTokenGeneration2024MustBeLongEnough";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    
    public static String generateToken(int userId, int profileId, String displayName) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("profileId", profileId)
            .claim("displayName", displayName)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(KEY)
            .compact();
    }
    
    public static Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }
    
    public static Integer getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return Integer.parseInt(claims.getSubject());
        }
        return null;
    }
    
    public static Integer getProfileIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("profileId", Integer.class);
        }
        return null;
    }
    
    public static String getDisplayNameFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("displayName", String.class);
        }
        return null;
    }
    
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
