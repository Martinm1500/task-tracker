package com.martin1500.service;

import com.martin1500.dto.*;
import com.martin1500.model.User;
import com.martin1500.model.util.Role;
import com.martin1500.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestAuthService {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void register_ValidRequest_ReturnsAuthResponseWithTokenPair() {
        String username = "newUser";
        String email = "newUser@example.com";
        String password = "securePassword";
        String encodedPassword = "encodedPassword";
        String accessToken = "mockAccessToken";
        String refreshToken = "mockRefreshToken";

        RegisterRequest request = new RegisterRequest(username, email, password);
        User user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        // Mocks
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateTokenPair(username)).thenReturn(new TokenPair(accessToken, refreshToken));

        AuthResponse response = authService.register(request);

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateTokenPair(username);

        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
    }

    @Test
    void login_ValidCredentials_ReturnsAuthResponseWithTokenPair() {
        String username = "testUser";
        String password = "testPassword";
        String accessToken = "mockAccessToken";
        String refreshToken = "mockRefreshToken";

        LoginRequest loginRequest = new LoginRequest(username, password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        // Mocks
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.generateTokenPair(username)).thenReturn(new TokenPair(accessToken, refreshToken));

        AuthResponse response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername(username);
        verify(jwtService).generateTokenPair(username);

        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
    }

    @Test
    void refreshToken_ValidRefreshToken_ReturnsNewAccessToken() {
        String refreshToken = "mockRefreshToken";
        String newAccessToken = "newMockAccessToken";
        RefreshRequest request = new RefreshRequest(refreshToken);

        // Mocks
        when(jwtService.refreshAccessToken(refreshToken)).thenReturn(newAccessToken);

        String result = authService.refreshToken(request);

        verify(jwtService).refreshAccessToken(refreshToken);

        assertNotNull(result);
        assertEquals(newAccessToken, result);
    }
}
