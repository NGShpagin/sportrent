package ru.saschoolsport.authz.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Kept out of the {@code @SpringBootApplication} class so that test slices
 * ({@code @DataJpaTest}, {@code @WebMvcTest}) do not try to build Feign clients.
 */
@Configuration
@EnableFeignClients(basePackages = "ru.saschoolsport.authz.client")
public class FeignClientsConfig {
}
