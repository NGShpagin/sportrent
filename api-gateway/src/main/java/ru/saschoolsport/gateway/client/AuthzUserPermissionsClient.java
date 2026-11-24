package ru.saschoolsport.gateway.client;

import ru.saschoolsport.gateway.config.GatewayAuthorizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthzUserPermissionsClient implements UserPermissionsClient {

    private static final ParameterizedTypeReference<List<String>> PERMISSION_NAMES =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient authorizationWebClient;
    private final GatewayAuthorizationProperties properties;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Mono<Set<String>> getPermissions(String userId) {
        CacheEntry cached = cache.get(userId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Mono.just(cached.permissions());
        }

        return authorizationWebClient.get()
                .uri("/internal/permissions?user_id={id}", userId)
                .retrieve()
                .bodyToMono(PERMISSION_NAMES)
                .map(names -> names == null ? Set.<String>of() : Set.copyOf(names))
                .doOnNext(permissions -> cache.put(userId, new CacheEntry(
                        permissions,
                        Instant.now().plus(properties.getCacheTtl()))))
                .onErrorMap(ex -> !(ex instanceof AuthorizationServiceUnavailableException), ex -> {
                    log.warn("Failed to load permissions for user {}: {}", userId, ex.getMessage());
                    return new AuthorizationServiceUnavailableException(ex);
                });
    }

    private record CacheEntry(Set<String> permissions, Instant expiresAt) {
    }
}
