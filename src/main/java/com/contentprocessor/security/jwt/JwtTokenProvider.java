package com.contentprocessor.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;


@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecretString;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationInMs;

    private Key jwtSecretKey;


    @PostConstruct
    protected void init() {
        try {
            // Convert string directly to bytes
            byte[] secretBytes = jwtSecretString.getBytes(StandardCharsets.UTF_8);

            // Ensure secret is at least 256 bits (32 bytes) for HS256
            // or 512 bits (64 bytes) for HS512
            if (secretBytes.length < 64) {
                logger.warn("JWT secret is shorter than recommended 64 bytes for HS512. Current length: {} bytes", secretBytes.length);
            }

            this.jwtSecretKey = Keys.hmacShaKeyFor(secretBytes);
            logger.info("JWT Secret Key initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize JWT secret!", e);
            throw new IllegalStateException("Failed to initialize JWT Secret Key", e);
        }
    }

    public String generateTokenFromUserId(String userId) {
        if (jwtSecretKey == null) {
            logger.error("JWT Secret Key is not initialized. Cannot generate token");
            throw new IllegalStateException("JWT Secret Key not initialized");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        logger.debug("Generating JWT for user ID: {} with expiry: {}", userId, expiryDate);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserIdFromJWT(String token) {
        if (jwtSecretKey == null) {
            logger.error("JWT Secret Key is not initialized. Cannot parse token.");
            throw new IllegalStateException("JWT Secret Key not initialized");
        }
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        if (jwtSecretKey == null) {
            logger.error("JWT Secret Key is not initialized. Cannot validate token.");
            return false;
        }
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecretKey).build().parseClaimsJws(authToken);
            logger.trace("JWT token validation successful.");
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty or invalid: {}", ex.getMessage());
        } catch (JwtException ex) {
            logger.error("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }
}