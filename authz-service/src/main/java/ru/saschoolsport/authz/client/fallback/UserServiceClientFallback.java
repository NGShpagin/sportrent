package ru.saschoolsport.authz.client.fallback;

import ru.saschoolsport.authz.client.UserServiceClient;
import ru.saschoolsport.authz.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallback implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("user-service unavailable: {}", cause.getMessage());
        return (id, username) -> {
            if (cause instanceof FeignException.NotFound) throw (FeignException.NotFound) cause;
            throw new ServiceUnavailableException("user-service is unavailable", cause);
        };
    }
}
