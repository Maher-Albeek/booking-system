package com.maher.booking_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Service
public class JwtService {
    private final byte[] secretBytes;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.security.jwt.secret:booking-system-dev-secret-change-me-please}") String secret,
            @Value("${app.security.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.secretBytes = normalizeSecret(secret).getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Long userId, String email, String role) {
        Objects.requireNonNull(userId, "userId must not be null");
        String safeRole = role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "email", email == null ? "" : email.trim().toLowerCase(),
                        "role", safeRole
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secretBytes))
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String normalizeSecret(String secret) {
        String safeSecret = secret == null ? "" : secret.trim();
        if (safeSecret.length() >= 32) {
            return safeSecret;
        }
        return (safeSecret + "booking-system-dev-secret-change-me-please").substring(0, 32);
    }
}
