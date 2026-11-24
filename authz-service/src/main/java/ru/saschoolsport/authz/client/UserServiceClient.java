package ru.saschoolsport.authz.client;

import ru.saschoolsport.authz.client.fallback.UserServiceClientFallback;
import ru.saschoolsport.authz.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "user-service", fallbackFactory = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/internal/users")
    UserDto getUser(@RequestParam(value = "userId", required = false) UUID id,
                    @RequestParam(value = "username", required = false) String username);
}
