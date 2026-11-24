package ru.saschoolsport.auth.client;

import ru.saschoolsport.auth.client.fallback.UserServiceClientFallback;
import ru.saschoolsport.auth.dto.LoginRequestDto;
import ru.saschoolsport.auth.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "user-service", fallbackFactory = UserServiceClientFallback.class)
public interface UserServiceClient {

    @PostMapping("/internal/auth/validate")
    UserDto authenticate(@RequestBody LoginRequestDto validateRequest);

    @GetMapping("/internal/users")
    UserDto getUser(@RequestParam(value = "userId", required = false) UUID userId,
                        @RequestParam(value = "username", required = false) String username);
}
