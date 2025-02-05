package com.martin1500.service;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.martin1500.exception.MissingSecretKeyException;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final int EXACT_LENGTH = 48;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secretKey:${JWT_SECRET_KEY}}")
    private String secretKey;

    @Value("${jwt.secretKeyBits:256}")
    private int keyLength;

    @Value("${jwt.expirationTime:1800000}")
    private long EXPIRATION_TIME;

    public String generateToken(String username) {
        return generateToken(Collections.emptyMap(), username);
    }

    public String generateToken(Map<String, Object> claims, String username) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

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



    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject().equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired for user: {}", userDetails.getUsername());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid token signature for user: {}", userDetails.getUsername());
            return false;
        } catch (JwtException e) {
            log.error("Invalid token for user: {}", userDetails.getUsername());
            return false;
        } catch (IllegalArgumentException | MissingSecretKeyException e) {
            log.error("SecretKey is not configured correctly", e);
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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

    public String renewToken(String token, UserDetails userDetails) {
        if (!isTokenValid(token, userDetails)) {
            throw new JwtException("Cannot renew an invalid or expired token.");
        }
        return generateToken(new HashMap<>(), userDetails.getUsername());
    }

}