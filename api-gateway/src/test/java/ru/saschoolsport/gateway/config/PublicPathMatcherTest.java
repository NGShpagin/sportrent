package ru.saschoolsport.gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPathMatcherTest {

    private static boolean isPublic(String path) {
        return PublicPathMatcher.isPublic(path,
                SecurityConfig.PUBLIC_EXACT_PATHS,
                SecurityConfig.PUBLIC_PREFIX_PATHS);
    }

    @Test
    void exactPathsArePublic() {
        assertThat(isPublic("/api/auth/login")).isTrue();
        assertThat(isPublic("/api/users/register")).isTrue();
        assertThat(isPublic("/actuator/health")).isTrue();
    }

    @Test
    void exactPathsDoNotMatchSubpaths() {
        assertThat(isPublic("/api/auth/login/extra")).isFalse();
        assertThat(isPublic("/actuator/health/liveness")).isFalse();
    }

    @Test
    void otherActuatorEndpointsAreNotPublic() {
        assertThat(isPublic("/actuator")).isFalse();
        assertThat(isPublic("/actuator/gateway/routes")).isFalse();
        assertThat(isPublic("/actuator/env")).isFalse();
        assertThat(isPublic("/actuator-evil")).isFalse();
    }

    @Test
    void prefixMatchingDoesNotAcceptLookAlikes() {
        String[] prefixes = {"/static"};
        assertThat(PublicPathMatcher.isPublic("/static", new String[0], prefixes)).isTrue();
        assertThat(PublicPathMatcher.isPublic("/static/app.js", new String[0], prefixes)).isTrue();
        assertThat(PublicPathMatcher.isPublic("/staticXYZ", new String[0], prefixes)).isFalse();
    }

    @Test
    void apiRoutesAreNotPublic() {
        assertThat(isPublic("/api/users")).isFalse();
    }
}
