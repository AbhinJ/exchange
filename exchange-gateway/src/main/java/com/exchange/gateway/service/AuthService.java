package com.exchange.gateway.service;

import com.exchange.gateway.security.*;
import com.exchange.gateway.security.dto.AuthResponse;
import com.exchange.gateway.security.dto.LoginRequest;
import com.exchange.gateway.security.dto.RefreshRequest;
import com.exchange.gateway.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .flatMap(existingUser -> Mono.<AuthResponse>error(
                        new RuntimeException("Username already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    var user = new User();
                    user.setUsername(request.getUsername());
                    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    user.setCreatedAt(Instant.now());
                    return userRepository.save(user)
                            .flatMap(this::generateTokenPair);
                }));
    }

    private Mono<AuthResponse> generateTokenPair(User user) {
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setCreatedAt(Instant.now());

        return refreshTokenRepository.save(refreshToken)
                .map(savedToken -> new AuthResponse(accessToken, savedToken.getToken(), user.getUsername()));
    }

    public Mono<Void> logout(RefreshRequest refreshRequest) {
        return refreshTokenRepository.deleteByToken(refreshRequest.getRefreshToken());
    }

    public Mono<AuthResponse> refresh(RefreshRequest refreshRequest) {
        return refreshTokenRepository.findByToken(refreshRequest.getRefreshToken())
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()))
                .flatMap(rt -> refreshTokenRepository.deleteByToken(rt.getToken())
                        .then(userRepository.findById(rt.getUserId())))
                .flatMap(this::generateTokenPair)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid or expired refresh token")));
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .flatMap(this::generateTokenPair)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid username or password")));
    }
}

