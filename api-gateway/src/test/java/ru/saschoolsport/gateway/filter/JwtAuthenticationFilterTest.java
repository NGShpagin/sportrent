package ru.saschoolsport.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ru.saschoolsport.gateway.config.JwtProperties;
import ru.saschoolsport.gateway.config.SecurityConfig;
import ru.saschoolsport.gateway.service.JwtService;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock WebFilterChain chain;

    SecretKey key;
    JwtAuthenticationFilter filter;

    private static byte[] randomKey() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    @BeforeEach
    void setUp() {
        byte[] secret = randomKey();
        key = Keys.hmacShaKeyFor(secret);
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(secret));
        JwtService jwtService = new JwtService(properties);
        jwtService.initSignKey();
        filter = new JwtAuthenticationFilter(jwtService, SecurityConfig.PUBLIC_EXACT_PATHS, SecurityConfig.PUBLIC_PREFIX_PATHS);
    }

    private String token(SecretKey signWith, long expiresInMillis) {
        return Jwts.builder()
                .subject("alice")
                .claim("userId", "user-123")
                .expiration(new Date(System.currentTimeMillis() + expiresInMillis))
                .signWith(signWith, Jwts.SIG.HS256)
                .compact();
    }

    private ServerWebExchange get(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get(path)
                .header("X-User-Id", "spoofed-by-client")
                .header("X-Username", "spoofed-by-client");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return MockServerWebExchange.from(request.build());
    }

    @Test
    void validTokenPassesAndGatewayHeadersOverwriteClientSuppliedOnes() {
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(get("/api/users", "Bearer " + token(key, 60_000)), chain).block();

        ArgumentCaptor<ServerWebExchange> forwarded = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(forwarded.capture());
        var headers = forwarded.getValue().getRequest().getHeaders();
        assertThat(headers.get("X-User-Id")).containsExactly("user-123");   // spoofed value is gone
        assertThat(headers.get("X-Username")).containsExactly("alice");
    }

    @Test
    void missingOrMalformedAuthorizationIs401() {
        for (String header : new String[]{null, "Basic abc", "Bearer"}) {
            ServerWebExchange exchange = get("/api/users", header);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).as("header=%s", header).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        verify(chain, never()).filter(any());
    }

    @Test
    void expiredTokenIs401() {
        ServerWebExchange exchange = get("/api/users", "Bearer " + token(key, -1_000));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() {
        SecretKey attackerKey = Keys.hmacShaKeyFor(randomKey());
        ServerWebExchange exchange = get("/api/users", "Bearer " + token(attackerKey, 60_000));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void publicPathNeedsNoToken() {
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(get("/api/auth/login", null), chain).block();
        filter.filter(get("/actuator/health", null), chain).block();

        verify(chain, org.mockito.Mockito.times(2)).filter(any());
    }

    @Test
    void otherActuatorPathsAreNotPublic() {
        ServerWebExchange exchange = get("/actuator/gateway/routes", null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
