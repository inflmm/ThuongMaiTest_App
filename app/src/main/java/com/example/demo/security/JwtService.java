package com.example.demo.security;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Generates and validates signed JWTs. This is the stateless replacement for
 * what HttpSession used to do implicitly — the token itself carries the
 * claims (username, role, userId), so nothing needs to be stored server-side
 * between requests.
 *
 * Written against jjwt 0.12.x's fluent builder/parser API — note this is a
 * different API shape than 0.11.x and earlier (setSubject/parserBuilder/
 * getBody are gone; this uses subject()/parser()/getPayload() instead).
 *
 * JWT_SECRET must be a long, random, base64-safe string kept in an
 * environment variable — never commit it. Rotating it invalidates every
 * outstanding token, so treat it like any other production secret.
 */
@Service
public class JwtService {

    private final Clock clock;

    public JwtService(Clock clock) {
        this.clock = clock;
    }

    @Value("${app.jwt.secret}")
    private String secretKey;

    // Access-token lifetime. Short by design — see the deployment doc's
    // access/refresh token discussion for why 15m–1h is normal here.
    @Value("${app.jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String username, String userId, String role) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiry = now.plus(Duration.ofMillis(expirationMs));

        return Jwts.builder()
                .subject(username)
                .claims(Map.of("userId", userId, "role", role))
            .issuedAt(toDate(now))
            .expiration(toDate(expiry))
                // No explicit algorithm needed — signWith(SecretKey) infers the
                // strongest suitable HMAC algorithm from the key's size.
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime expiration = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), clock.getZone());
            return expiration.isAfter(now);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date toDate(LocalDateTime dateTime) {
        ZoneId zone = clock.getZone();
        return Date.from(dateTime.atZone(zone).toInstant());
    }
}
