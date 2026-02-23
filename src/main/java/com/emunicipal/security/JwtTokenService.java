package com.emunicipal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes:120}") long expirationMinutes,
            @Value("${security.jwt.issuer:e-municipal-corporation-system}") String issuer) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = Math.max(1, expirationMinutes);
        this.issuer = issuer;
    }

    public IssuedToken issueToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .subject(principal.loginId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("uid", principal.userId())
                .claim("role", principal.role().name())
                .claim("type", principal.principalType())
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = claims.get("uid", Long.class);
        String roleValue = claims.get("role", String.class);
        String principalType = claims.get("type", String.class);
        String loginId = claims.getSubject();

        AppRole role = AppRole.fromInput(roleValue);
        if (userId == null || loginId == null || role == null) {
            throw new JwtException("Token is missing required claims");
        }

        return new AuthPrincipal(userId, loginId, role, principalType == null ? "UNKNOWN" : principalType);
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        if (!header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? null : token;
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
