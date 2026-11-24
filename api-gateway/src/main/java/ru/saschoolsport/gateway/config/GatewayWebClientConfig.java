package ru.saschoolsport.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayWebClientConfig {

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient authorizationWebClient(
            WebClient.Builder loadBalancedWebClientBuilder,
            AppProperties appProperties
    ) {
        return loadBalancedWebClientBuilder
                .baseUrl("http://authz-service")
                .defaultHeader("X-Internal-Api-Key", appProperties.getInternalApiKey())
                .build();
    }
}
