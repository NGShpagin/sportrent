package ru.saschoolsport.auth.service;

import ru.saschoolsport.auth.config.JwtConfig;
import ru.saschoolsport.auth.config.ServiceProperties;
import ru.saschoolsport.auth.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;
    private final ServiceProperties serviceProperties;

    private SecretKey getSignKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public UUID extractSessionId(String token) {
        String raw = extractClaim(token, claims -> claims.get("sessionId", String.class));
        return raw != null ? UUID.fromString(raw) : null;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())  // 👈 Используем getSignKey() напрямую
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        final String extractedLogin = extractUsername(token);
        return (extractedLogin.equals(username) && !isTokenExpired(token));
    }

    public String generateToken(UserDto user, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("sessionId", sessionId.toString());
        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + serviceProperties.getAccessTokenTtl().toMillis()))
                .signWith(getSignKey(), Jwts.SIG.HS256)  // 👈 Используем getSignKey() напрямую
                .compact();
    }
}
