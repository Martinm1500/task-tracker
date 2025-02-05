package com.martin1500.service;

import com.martin1500.dto.AuthResponse;
import com.martin1500.dto.LoginRequest;
import com.martin1500.dto.RegisterRequest;
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
    void register_ValidRequest_ReturnsAuthResponseWithToken() {
        String username = "newUser";
        String email = "newUser@example.com";
        String password = "securePassword";
        String encodedPassword = "encodedPassword";
        String generatedToken = "mockToken";

        RegisterRequest request = new RegisterRequest(username, email, password);
        User user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        // Mocks
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateToken(username)).thenReturn(generatedToken);

        AuthResponse response = authService.register(request);

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(username);

        assertNotNull(response);
        assertEquals(generatedToken, response.token());
    }

    @Test
    void login_ValidCredentials_ReturnsAuthResponseWithToken() {
        String username = "testUser";
        String password = "testPassword";
        String generatedToken = "mockToken";

        LoginRequest loginRequest = new LoginRequest(username, password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        // Mocks
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(username)).thenReturn(generatedToken);


        AuthResponse response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername(username);
        verify(jwtService).generateToken(username);

        assertNotNull(response);
        assertEquals(generatedToken, response.token());
    }
}
