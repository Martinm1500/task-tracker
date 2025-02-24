package com.martin1500.service;

import com.martin1500.TaskTrackerApplication;

import com.martin1500.dto.TokenPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TaskTrackerApplication.class)
@ExtendWith(MockitoExtension.class)
public class TestJwtService {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateTokenPair_Success() throws Exception {
        // Given
        String username = "testUser";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");

        // Access the private getSignKey() method using reflection
        Method getSignKeyMethod = jwtService.getClass().getDeclaredMethod("getSignKey");
        getSignKeyMethod.setAccessible(true);
        Key signKey = (Key) getSignKeyMethod.invoke(jwtService);

        // When
        TokenPair tokenPair = jwtService.generateTokenPair(username);

        // Then
        assertNotNull(tokenPair.accessToken(), "The generated access token should not be null");
        assertNotNull(tokenPair.refreshToken(), "The generated refresh token should not be null");

        // Verify access token
        Claims accessClaims = Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(tokenPair.accessToken())
                .getBody();
        assertEquals(username, accessClaims.getSubject(), "Access token subject should match username");
        assertTrue(accessClaims.getExpiration().after(new Date()), "Access token should not be expired");

        // Verify refresh token
        Claims refreshClaims = Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(tokenPair.refreshToken())
                .getBody();
        assertEquals(username, refreshClaims.getSubject(), "Refresh token subject should match username");
        assertTrue(refreshClaims.getExpiration().after(new Date()), "Refresh token should not be expired");
    }

    @Test
    void generateToken_WithClaims_Success() throws Exception {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String username = "testUser";

        // Access the private getSignKey() method using reflection
        Method getSignKeyMethod = jwtService.getClass().getDeclaredMethod("getSignKey");
        getSignKeyMethod.setAccessible(true);
        Key signKey = (Key) getSignKeyMethod.invoke(jwtService);

        // When
        String accessToken = jwtService.generateAccessToken(username);

        // Then
        assertNotNull(accessToken, "The generated access token should not be null");
        Claims parsedClaims = Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
        assertEquals(username, parsedClaims.getSubject(), "Token subject should match username");
        assertTrue(parsedClaims.getExpiration().after(new Date()), "Token should not be expired");
    }

    @Test
    void shouldFailWhenClaimsAreNull() {
        // Given
        String username = "testUser";

        // When & Then
        String accessToken = jwtService.generateAccessToken(username);
        assertNotNull(accessToken, "Token should still be generated with null claims treated as empty");
    }

    @Test
    void shouldFailWhenUsernameIsNull() throws Exception {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");

        // Access the private generateToken method using reflection
        Method generateTokenMethod = jwtService.getClass().getDeclaredMethod("generateToken", Map.class, String.class, long.class);
        generateTokenMethod.setAccessible(true);

        // When & Then
        Exception exception = assertThrows(InvocationTargetException.class, () -> {
            generateTokenMethod.invoke(jwtService, claims, null, 900000L);
        });
        Throwable cause = exception.getCause();
        assertInstanceOf(IllegalArgumentException.class, cause, "Cause should be IllegalArgumentException");
        assertEquals("Username cannot be null or empty", cause.getMessage(), "Exception message should match");
    }

    @Test
    void refreshAccessToken_Success() throws Exception {
        // Given
        String username = "testUser";
        TokenPair tokenPair = jwtService.generateTokenPair(username);

        // Access the private getSignKey() method using reflection
        Method getSignKeyMethod = jwtService.getClass().getDeclaredMethod("getSignKey");
        getSignKeyMethod.setAccessible(true);
        Key signKey = (Key) getSignKeyMethod.invoke(jwtService);

        // When
        String newAccessToken = jwtService.refreshAccessToken(tokenPair.refreshToken());

        // Then
        assertNotNull(newAccessToken, "The new access token should not be null");
        Claims newClaims = Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(newAccessToken)
                .getBody();
        assertEquals(username, newClaims.getSubject(), "New access token subject should match username");
        assertTrue(newClaims.getExpiration().after(new Date()), "New access token should not be expired");
    }

    @Test
    void refreshAccessToken_ShouldFailWhenRefreshTokenExpired() throws Exception {
        // Given
        String username = "testUser";

        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000);

        Method getSignKeyMethod = jwtService.getClass().getDeclaredMethod("getSignKey");
        getSignKeyMethod.setAccessible(true);
        Key signKey = (Key) getSignKeyMethod.invoke(jwtService);

        String expiredRefreshToken = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiredDate)
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();

        // When & Then
        Exception exception = assertThrows(JwtException.class, () -> {
            jwtService.refreshAccessToken(expiredRefreshToken);
        });
        assertEquals("Refresh token has expired", exception.getMessage(), "Exception message should match");
    }
}