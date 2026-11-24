package ru.saschoolsport.gateway.filter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import ru.saschoolsport.gateway.client.AuthorizationServiceUnavailableException;
import ru.saschoolsport.gateway.client.UserPermissionsClient;
import ru.saschoolsport.gateway.config.GatewayAuthorizationProperties;
import ru.saschoolsport.gateway.config.SecurityConfig;
import ru.saschoolsport.gateway.filter.PermissionAuthorizationFilter.AuthorizationMatch.Kind;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Uses the real gateway.authorization.rules from application.yml, so config regressions are caught. */
@ExtendWith(MockitoExtension.class)
class PermissionAuthorizationFilterTest {

    static GatewayAuthorizationProperties properties;

    @Mock UserPermissionsClient permissionsClient;
    @Mock WebFilterChain chain;
    PermissionAuthorizationFilter filter;

    @BeforeAll
    static void loadRealRules() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        properties = new Binder(ConfigurationPropertySources.from(sources))
                .bind("gateway.authorization", GatewayAuthorizationProperties.class)
                .orElseThrow(() -> new AssertionError("gateway.authorization missing from application.yml"));
    }

    @BeforeEach
    void setUp() {
        filter = new PermissionAuthorizationFilter(
                SecurityConfig.PUBLIC_EXACT_PATHS, SecurityConfig.PUBLIC_PREFIX_PATHS, properties, permissionsClient);
    }

    private void assertRule(HttpMethod method, String path, Kind kind, String permission) {
        PermissionAuthorizationFilter.AuthorizationMatch match = filter.match(method, path);
        assertThat(match.kind()).as("%s %s", method, path).isEqualTo(kind);
        assertThat(match.permission()).as("%s %s permission", method, path).isEqualTo(permission);
    }

    // ---- rule table ----

    @Test
    void userEndpointsMapToUserPermissions() {
        assertRule(HttpMethod.GET, "/api/users", Kind.REQUIRE, "users:read");
        assertRule(HttpMethod.GET, "/api/users/123", Kind.REQUIRE, "users:read");
        assertRule(HttpMethod.POST, "/api/users", Kind.REQUIRE, "users:create");
        assertRule(HttpMethod.PUT, "/api/users/123", Kind.REQUIRE, "users:update");
        assertRule(HttpMethod.DELETE, "/api/users/123", Kind.REQUIRE, "users:delete");
    }

    @Test
    void roleAndPermissionManagementRequiresRolesManage_includingNewEndpoints() {
        assertRule(HttpMethod.GET, "/api/roles", Kind.REQUIRE, "roles:manage");
        assertRule(HttpMethod.POST, "/api/roles", Kind.REQUIRE, "roles:manage");
        assertRule(HttpMethod.POST, "/api/roles/5/permissions", Kind.REQUIRE, "roles:manage");   // grant permissions
        assertRule(HttpMethod.PUT, "/api/roles/7/users/abc", Kind.REQUIRE, "roles:manage");      // assign role
        assertRule(HttpMethod.DELETE, "/api/roles/7/users/abc", Kind.REQUIRE, "roles:manage");   // revoke role
        assertRule(HttpMethod.GET, "/api/permissions", Kind.REQUIRE, "roles:manage");
        assertRule(HttpMethod.POST, "/api/permissions", Kind.REQUIRE, "roles:manage");           // create permission
    }

    @Test
    void currentUserEndpointsOnlyNeedAuthentication() {
        assertRule(HttpMethod.GET, "/api/me/profile", Kind.SKIP, null);
        assertRule(HttpMethod.GET, "/api/me/permissions", Kind.SKIP, null);
    }

    @Test
    void everythingNotExplicitlyAllowedIsDenied() {
        assertRule(HttpMethod.GET, "/api/unknown", Kind.DENY, null);
        assertRule(HttpMethod.PATCH, "/api/users/1", Kind.DENY, null);
        assertRule(HttpMethod.POST, "/api/me/profile", Kind.DENY, null);
        assertRule(HttpMethod.DELETE, "/api/permissions/1", Kind.REQUIRE, "roles:manage"); // covered by wildcard rule
    }

    // ---- filter behaviour ----

    private ServerWebExchange exchange(HttpMethod method, String path, String userId) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.method(method, path);
        if (userId != null) {
            request.header("X-User-Id", userId);
        }
        return MockServerWebExchange.from(request.build());
    }

    @Test
    void grantedPermissionPassesThrough() {
        when(permissionsClient.getPermissions("u1")).thenReturn(Mono.just(Set.of("users:read")));
        when(chain.filter(any())).thenReturn(Mono.empty());
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users", "u1");

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void missingPermissionIs403() {
        when(permissionsClient.getPermissions("u1")).thenReturn(Mono.just(Set.of("users:read")));
        ServerWebExchange exchange = exchange(HttpMethod.DELETE, "/api/users/9", "u1");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void missingUserIdHeaderIs401() {
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users", null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(permissionsClient);
        verify(chain, never()).filter(any());
    }

    @Test
    void authzServiceDownIs503NotAnImplicitAllow() {
        when(permissionsClient.getPermissions("u1"))
                .thenReturn(Mono.error(new AuthorizationServiceUnavailableException(new RuntimeException("boom"))));
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users", "u1");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(chain, never()).filter(any());
    }

    @Test
    void unknownApiPathIs403EvenForAuthenticatedUsers() {
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/unknown", "u1");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void publicAndNonApiPathsSkipTheCheck() {
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange(HttpMethod.POST, "/api/auth/login", null), chain).block();
        filter.filter(exchange(HttpMethod.GET, "/something-else", null), chain).block();

        verifyNoInteractions(permissionsClient);
    }
}
