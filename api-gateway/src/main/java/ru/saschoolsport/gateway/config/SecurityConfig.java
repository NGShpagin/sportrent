package ru.saschoolsport.gateway.config;

import ru.saschoolsport.gateway.client.UserPermissionsClient;
import ru.saschoolsport.gateway.filter.JwtAuthenticationFilter;
import ru.saschoolsport.gateway.filter.PermissionAuthorizationFilter;
import ru.saschoolsport.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Exact paths that do not require authentication.
     * Used by both the JWT filter and the Spring Security authorization rules —
     * this is the single source of truth for the public-path whitelist.
     */
    public static final String[] PUBLIC_EXACT_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/users/register",
            "/actuator/health"
    };

    /**
     * Path prefixes (without trailing slash) that do not require authentication.
     * Currently none; kept so both filters and the security rules stay driven by one list.
     */
    public static final String[] PUBLIC_PREFIX_PATHS = {};

    private final JwtService jwtService;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, PUBLIC_EXACT_PATHS, PUBLIC_PREFIX_PATHS);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GatewayAuthorizationProperties authorizationProperties,
            UserPermissionsClient userPermissionsClient
    ) {
        PermissionAuthorizationFilter permissionAuthorizationFilter = new PermissionAuthorizationFilter(
                PUBLIC_EXACT_PATHS,
                PUBLIC_PREFIX_PATHS,
                authorizationProperties,
                userPermissionsClient
        );

        // Build the full permit-all pattern list from the same constants the filter uses.
        String[] prefixPatterns = Arrays.stream(PUBLIC_PREFIX_PATHS)
                .flatMap(p -> Stream.of(p, p + "/**"))
                .toArray(String[]::new);
        String[] allPublicPatterns = Stream.concat(
                Arrays.stream(PUBLIC_EXACT_PATHS),
                Arrays.stream(prefixPatterns)
        ).toArray(String[]::new);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(allPublicPatterns).permitAll()
                        .pathMatchers("/api/me/**").authenticated()
                        .pathMatchers("/internal/**").denyAll()
                        // Only /actuator/health is public (see PUBLIC_EXACT_PATHS); everything else
                        // under /actuator is closed even to authenticated users.
                        .pathMatchers("/actuator/**").denyAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(permissionAuthorizationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
