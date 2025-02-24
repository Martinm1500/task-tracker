package com.martin1500.service;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

import com.martin1500.dto.TokenPair;
import com.martin1500.exception.MissingSecretKeyException;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final int EXACT_LENGTH = 48;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secretKey:${JWT_SECRET_KEY}}")
    private String secretKey;

    @Value("${jwt.accessTokenExpiration:900000}")
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${jwt.refreshTokenExpiration:604800000}")
    private long REFRESH_TOKEN_EXPIRATION;

    public TokenPair generateTokenPair(String username) {
        return new TokenPair(generateAccessToken(username), generateRefreshToken(username));
    }

    public String generateAccessToken(String username) {
        return generateToken(Collections.emptyMap(), username, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String username) {
        return generateToken(Collections.emptyMap(), username, REFRESH_TOKEN_EXPIRATION);
    }

    private String generateToken(Map<String, Object> claims, String username, long expirationTime) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationTime);

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSignKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (SecurityException e) {
            log.error("Error generating JWT token: {}", e.getMessage());
            throw new JwtException("Failed to generate JWT token due to security issues.");
        }
    }

    private Key getSignKey() {
        if (secretKey == null) {
            throw new MissingSecretKeyException("The JWT_SECRET_KEY environment variable must be set.");
        }
        if (secretKey.length() != EXACT_LENGTH) {
            log.error("The secret key must be {} characters long.", EXACT_LENGTH);
            throw new IllegalArgumentException("The secret key must be " + EXACT_LENGTH + " characters long.");
        }
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String refreshAccessToken(String refreshToken) {
        try {
            if (isTokenExpired(refreshToken)) {
                throw new JwtException("Refresh token has expired");
            }
            String username = extractUsername(refreshToken);
            return generateAccessToken(username);
        } catch (ExpiredJwtException e) {
            throw new JwtException("Refresh token has expired");
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
}