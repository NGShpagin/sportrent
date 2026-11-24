package ru.saschoolsport.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.authorization")
public class GatewayAuthorizationProperties {

    private boolean enabled = true;
    private Duration cacheTtl = Duration.ofSeconds(45);
    private List<Rule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class Rule {
        private List<HttpMethod> methods = new ArrayList<>();
        private List<String> paths = new ArrayList<>();
        /** Blank means authenticated-only: JWT is enough, no permission check. */
        private String permission;
    }
}
