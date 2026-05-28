package com.exchange.gateway.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken {
    @Id
    private Long id;
    private Long userId;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;
}

