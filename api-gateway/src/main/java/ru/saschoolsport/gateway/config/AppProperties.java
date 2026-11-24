package ru.saschoolsport.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Shared secret sent to internal services in the X-Internal-Api-Key header. */
    private String internalApiKey;
}
