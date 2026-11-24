package ru.saschoolsport.gateway.filter;

import ru.saschoolsport.gateway.config.PublicPathMatcher;
import ru.saschoolsport.gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    /** Headers injected by the gateway — must be stripped from client requests to prevent forgery. */
    private static final List<String> GATEWAY_HEADERS = List.of("X-Username", "X-User-Id");

    private final JwtService jwtService;
    private final String[] publicExactPaths;
    private final String[] publicPrefixPaths;

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing bearer token for path: {}", path);
            return reject(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        // Single HMAC parse — validates signature, expiry, and extracts all claims at once.
        Optional<Claims> claimsOpt = jwtService.extractValidClaims(token);
        if (claimsOpt.isEmpty()) {
            log.debug("Invalid or expired token for path: {}", path);
            return reject(exchange, "Invalid or expired token");
        }

        Claims claims = claimsOpt.get();
        String username = claims.getSubject();
        Object userIdRaw = claims.get("userId");
        String userId = userIdRaw != null ? userIdRaw.toString() : null;

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContext securityContext = new SecurityContextImpl(authentication);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request
                        // Strip any client-supplied gateway headers to prevent header injection.
                        .headers(headers -> GATEWAY_HEADERS.forEach(headers::remove))
                        .header("X-Username", username)
                        .header("X-User-Id", userId))
                .build();

        return chain.filter(mutatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    boolean isPublicEndpoint(String path) {
        return PublicPathMatcher.isPublic(path, publicExactPaths, publicPrefixPaths);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
