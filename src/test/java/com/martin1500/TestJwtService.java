package com.martin1500;

import com.martin1500.service.JwtService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = TaskTrackerApplication.class)
@ExtendWith(MockitoExtension.class)
public class TestJwtService {

    @Value("${jwt.secretKey:${JWT_SECRET_KEY}}")
    private String secretKey;
    @Autowired
    private JwtService jwtService;

    @Test
    void GenerateToken_Success() throws Exception {

        System.out.println(secretKey);
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String username = "testUser";

        // Access the private getSignKey() method using reflection
        Method getSignKeyMethod = jwtService.getClass().getDeclaredMethod("getSignKey");
        getSignKeyMethod.setAccessible(true);
        Key signKey = (Key) getSignKeyMethod.invoke(jwtService);

        // When
        String token = jwtService.generateToken(claims, username);

        // Then
        assertNotNull(token, "The generated token should not be null");
        assertDoesNotThrow(() -> Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(token), "The token should be valid and parseable");
    }
}
