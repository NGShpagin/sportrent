package ru.saschoolsport.gateway.filter;

import ru.saschoolsport.gateway.client.AuthorizationServiceUnavailableException;
import ru.saschoolsport.gateway.client.UserPermissionsClient;
import ru.saschoolsport.gateway.config.GatewayAuthorizationProperties;
import ru.saschoolsport.gateway.config.PublicPathMatcher;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class PermissionAuthorizationFilter implements WebFilter {

    private static final PathPatternParser PATH_PARSER = new PathPatternParser();

    private final String[] publicExactPaths;
    private final String[] publicPrefixPaths;
    private final GatewayAuthorizationProperties properties;
    private final UserPermissionsClient permissionsClient;
    private final List<CompiledRule> compiledRules;

    public PermissionAuthorizationFilter(
            String[] publicExactPaths,
            String[] publicPrefixPaths,
            GatewayAuthorizationProperties properties,
            UserPermissionsClient permissionsClient
    ) {
        this.publicExactPaths = publicExactPaths;
        this.publicPrefixPaths = publicPrefixPaths;
        this.properties = properties;
        this.permissionsClient = permissionsClient;
        this.compiledRules = properties.getRules().stream()
                .map(CompiledRule::from)
                .toList();
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        HttpMethod method = exchange.getRequest().getMethod();
        AuthorizationMatch match = match(method, path);
        return switch (match.kind()) {
            case SKIP -> chain.filter(exchange);
            case DENY -> reject(exchange, HttpStatus.FORBIDDEN, "Forbidden");
            case REQUIRE -> authorize(exchange, chain, match.permission());
        };
    }

    AuthorizationMatch match(HttpMethod method, String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        for (CompiledRule rule : compiledRules) {
            if (rule.matches(method, pathContainer)) {
                if (rule.permission() == null || rule.permission().isBlank()) {
                    return AuthorizationMatch.skip();
                }
                return AuthorizationMatch.require(rule.permission());
            }
        }
        return AuthorizationMatch.deny();
    }

    boolean isPublicEndpoint(String path) {
        return PublicPathMatcher.isPublic(path, publicExactPaths, publicPrefixPaths);
    }

    private Mono<Void> authorize(ServerWebExchange exchange, WebFilterChain chain, String permission) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing user identity");
        }

        return permissionsClient.getPermissions(userId)
                .flatMap(granted -> {
                    if (granted.contains(permission)) {
                        return chain.filter(exchange);
                    }
                    log.debug("User {} is missing permission {} for {} {}",
                            userId, permission, exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
                    return reject(exchange, HttpStatus.FORBIDDEN, "Forbidden");
                })
                .onErrorResume(AuthorizationServiceUnavailableException.class,
                        ex -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Authorization service unavailable"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    record AuthorizationMatch(Kind kind, String permission) {
        enum Kind { SKIP, REQUIRE, DENY }

        static AuthorizationMatch skip() {
            return new AuthorizationMatch(Kind.SKIP, null);
        }

        static AuthorizationMatch deny() {
            return new AuthorizationMatch(Kind.DENY, null);
        }

        static AuthorizationMatch require(String permission) {
            return new AuthorizationMatch(Kind.REQUIRE, permission);
        }
    }

    private record CompiledRule(List<HttpMethod> methods, List<PathPattern> patterns, String permission) {
        static CompiledRule from(GatewayAuthorizationProperties.Rule rule) {
            List<PathPattern> patterns = rule.getPaths().stream()
                    .map(PATH_PARSER::parse)
                    .toList();
            return new CompiledRule(
                    rule.getMethods() == null ? List.of() : List.copyOf(rule.getMethods()),
                    patterns,
                    rule.getPermission()
            );
        }

        boolean matches(HttpMethod method, PathContainer path) {
            if (!methods.isEmpty() && (method == null || !methods.contains(method))) {
                return false;
            }
            return patterns.stream().anyMatch(pattern -> pattern.matches(path));
        }
    }
}
