package ru.saschoolsport.user.controller;

import ru.saschoolsport.user.dto.*;
import ru.saschoolsport.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/internal/auth/validate")
    public ResponseEntity<UserDto> validateCredentials(@Valid @RequestBody ValidateRequestDto request) {
        log.info("Validating credentials for user: {}", request.getUsername());
        try {
            UserDto dto = UserDto.fromEntity(userService.validateCredentials(request.getUsername(), request.getPassword()));
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            // Always return 401 — never reveal whether user or password was wrong
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/internal/users")
    public ResponseEntity<UserDto> getInternalUser(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "username", required = false) String username) {

        if ((userId == null && username == null) || (userId != null && username != null)) {
            throw new IllegalArgumentException("Exactly one of userId or username must be provided");
        }

        return ResponseEntity.ok(userService.getInternalUser(userId, username));
    }

    @GetMapping("/internal/users/contact-info")
    public ResponseEntity<List<UserContactInfoDto>> getInternalUserContactInfo(
            @RequestParam(value = "userIds", required = true) List<UUID> userIds) {

        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("Exactly one userId must be provided");
        }

        return ResponseEntity.ok(userService.getUserContactInfo(userIds));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/users")
    public ResponseEntity<PagedUserResponse> getUsersList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort_by", defaultValue = "updatedAt") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(PagedUserResponse.from(userService.getUsers(page, size, sortBy, sortDir)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Void> updateUser(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        RegisterResponse body = RegisterResponse.fromEntity(userService.createUser(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileDto> profile(
            @RequestHeader(value = "X-User-Id") String requestUserId) {

        if (requestUserId == null || requestUserId.isBlank()) {
            throw new IllegalArgumentException("Missing or empty X-User-Id header");
        }

        try {
            UUID userId = UUID.fromString(requestUserId);
            return ResponseEntity.ok(userService.getUserProfile(userId));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid X-User-Id format: " + requestUserId);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
