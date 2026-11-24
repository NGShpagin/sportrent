package ru.saschoolsport.auth.exception;

import ru.saschoolsport.auth.dto.ErrorResponseDto;
import feign.FeignException;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleAuthFailed(RuntimeException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler({EntityNotFoundException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDto> handleAuthError(RuntimeException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponseDto> handleJwt(JwtException ex) {
        log.debug("JWT validation failed: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token", ex.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponseDto> handleFeign(FeignException ex) {
        int status = ex.status();
        if (status == 401 || status == 403 || status == 404) {
            return error(HttpStatus.UNAUTHORIZED, "Authentication failed", "User not found or invalid credentials");
        }
        log.error("Downstream service error: {} {}", status, ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", "Downstream service is unreachable");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(ServiceUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, "Bad request", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponseDto> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponseDto(LocalDateTime.now(), status.value(), error, message, null)
        );
    }
}
