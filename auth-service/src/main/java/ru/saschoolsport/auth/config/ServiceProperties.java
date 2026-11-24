package ru.saschoolsport.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class ServiceProperties {
    private Duration accessTokenTtl;
    private Duration refreshTokenTtl;
    private Duration sessionTtl;
}