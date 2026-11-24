package ru.saschoolsport.auth.controller;

import ru.saschoolsport.auth.config.ServiceProperties;
import ru.saschoolsport.auth.dto.LoginRequestDto;
import ru.saschoolsport.auth.dto.LoginResponse;
import ru.saschoolsport.auth.service.AuthService;
import ru.saschoolsport.auth.service.JwtService;
import ru.saschoolsport.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final ServiceProperties serviceProperties;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
                                      HttpServletResponse response) {
        log.info("Login attempt for user: {}", loginRequestDto.getUsername());
        LoginResponse loginResponse = authService.authenticate(
                loginRequestDto.getUsername(), loginRequestDto.getPassword());

        addRefreshCookie(response, loginResponse.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken())
                .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION)
                .build();
    }

    @PostMapping("/internal/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        // Throws JwtException if signature/format invalid → RestExceptionHandler → 401
        UUID sessionId = jwtService.extractSessionId(token);
        if (sessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Throws EntityNotFoundException / IllegalStateException → RestExceptionHandler → 401
        sessionService.validateSession(sessionId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponse loginResponse = authService.refresh(refreshToken);
        addRefreshCookie(response, loginResponse.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken())
                .header("Access-Control-Expose-Headers", HttpHeaders.AUTHORIZATION)
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) serviceProperties.getRefreshTokenTtl().toSeconds());
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
