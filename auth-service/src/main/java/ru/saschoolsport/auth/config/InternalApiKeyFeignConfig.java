package ru.saschoolsport.auth.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class InternalApiKeyFeignConfig {

    @Bean
    @Profile("!test")
    RequestInterceptor internalApiKeyInterceptor(@Value("${app.internal-api-key}") String key) {
        return template -> {
            if (key != null && !key.isBlank()) {
                template.header(InternalApiKeyFilter.HEADER, key);
            }
        };
    }
}
