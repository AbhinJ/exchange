package com.exchange.gateway.controller;

import com.exchange.gateway.security.dto.AuthResponse;
import com.exchange.gateway.security.dto.LoginRequest;
import com.exchange.gateway.security.dto.RefreshRequest;
import com.exchange.gateway.security.dto.RegisterRequest;
import com.exchange.gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@RequestBody RefreshRequest request) {
        return authService.logout(request);
    }
}
