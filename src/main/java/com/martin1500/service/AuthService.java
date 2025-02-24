package com.martin1500.service;

import com.martin1500.dto.*;
import com.martin1500.model.User;
import com.martin1500.model.util.Role;
import com.martin1500.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Username Not Found."));

        TokenPair tokenPair = jwtService.generateTokenPair(user.getUsername());
        return new AuthResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        TokenPair tokenPair = jwtService.generateTokenPair(user.getUsername());
        return new AuthResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    public String refreshToken(RefreshRequest request) {
        return jwtService.refreshAccessToken(request.refreshToken());
    }
}
