package ru.saschoolsport.auth.client.fallback;

import ru.saschoolsport.auth.client.UserServiceClient;
import ru.saschoolsport.auth.exception.ServiceUnavailableException;
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
        return new UserServiceClient() {
            @Override
            public ru.saschoolsport.auth.dto.UserDto authenticate(
                    ru.saschoolsport.auth.dto.LoginRequestDto request) {
                if (cause instanceof FeignException.NotFound) throw (FeignException.NotFound) cause;
                if (cause instanceof FeignException.Unauthorized) throw (FeignException.Unauthorized) cause;
                throw new ServiceUnavailableException("user-service is unavailable", cause);
            }

            @Override
            public ru.saschoolsport.auth.dto.UserDto getUser(java.util.UUID userId, String username) {
                if (cause instanceof FeignException.NotFound) throw (FeignException.NotFound) cause;
                throw new ServiceUnavailableException("user-service is unavailable", cause);
            }
        };
    }
}
