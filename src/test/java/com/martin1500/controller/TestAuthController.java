package com.martin1500.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.martin1500.dto.AuthResponse;
import com.martin1500.dto.LoginRequest;
import com.martin1500.dto.RegisterRequest;
import com.martin1500.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class TestAuthController {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        authResponse = new AuthResponse("token123");
    }

    @Test
    void testRegister_WithValidCredentials_ShouldReturnTokenAndOkStatus() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("user", "test@example.com", "password123");
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        verify(authService).register(any(RegisterRequest.class));
    }


    @Test
    void testLogin_WithValidCredentials_ShouldReturnTokenAndOkStatus() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        verify(authService).login(any(LoginRequest.class));
    }
}