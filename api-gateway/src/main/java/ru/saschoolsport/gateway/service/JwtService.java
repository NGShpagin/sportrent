package ru.saschoolsport.gateway.service;

import ru.saschoolsport.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey signKey;

    @PostConstruct
    public void initSignKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.signKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parses the token, verifies the signature, and checks expiry in one operation.
     * Returns the claims on success, or empty if the token is invalid/expired.
     * Prefer this over separate validateToken + extract* calls to avoid redundant HMAC operations.
     */
    public Optional<Claims> extractValidClaims(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().after(new Date())) {
                return Optional.of(claims);
            }
            log.debug("Token expired");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
