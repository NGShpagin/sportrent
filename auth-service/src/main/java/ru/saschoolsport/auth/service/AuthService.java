package ru.saschoolsport.auth.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.saschoolsport.auth.client.UserServiceClient;
import ru.saschoolsport.auth.dto.LoginRequestDto;
import ru.saschoolsport.auth.dto.LoginResponse;
import ru.saschoolsport.auth.dto.UserDto;
import ru.saschoolsport.auth.entity.RefreshToken;
import ru.saschoolsport.auth.entity.Session;
import ru.saschoolsport.auth.exception.RefreshTokenReuseException;

import java.util.UUID;

/**
 * Remote calls to user-service are made <b>outside</b> any DB transaction so a slow or hung
 * downstream never pins a connection. DB work is done in short {@link TransactionTemplate}
 * blocks around the remote call; this class is intentionally not {@code @Transactional}
 * at the method level for {@code authenticate} and {@code refresh}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final TransactionTemplate transactionTemplate;

    public LoginResponse authenticate(String username, String password) {
        log.info("Authenticating user: {}", username);

        // 1. Remote call, no transaction.
        UserDto user;
        try {
            user = userServiceClient.authenticate(new LoginRequestDto(username, password));
        } catch (FeignException.NotFound | FeignException.Unauthorized | FeignException.Forbidden e) {
            // One message for all three: never reveal whether the user or the password was wrong.
            throw new BadCredentialsException("Invalid username or password");
        }
        // Other FeignException (5xx, network) propagates as-is -> handled as 503 by RestExceptionHandler

        // 2. Session + refresh token are created atomically.
        return transactionTemplate.execute(status -> {
            Session session = sessionService.createSession(user.getId());
            String accessToken = jwtService.generateToken(user, session.getId());
            String refreshToken = refreshTokenService.create(user.getId(), session).getToken();
            return new LoginResponse(accessToken, refreshToken);
        });
    }

    /**
     * Rotates a refresh token. Presenting an already rotated token means a stale copy is in
     * circulation (theft, or a client that lost the response), so the whole session is closed:
     * every token of that session stops working and the user has to log in again.
     * The close runs after the failed transaction is rolled back, in its own transaction.
     */
    public LoginResponse refresh(String refreshToken) {
        try {
            return doRefresh(refreshToken);
        } catch (RefreshTokenReuseException e) {
            log.warn("Rotated refresh token reused, closing session {}", e.getSessionId());
            sessionService.closeSession(e.getSessionId());
            throw e;
        }
    }

    private LoginResponse doRefresh(String refreshToken) {
        // 1. Cheap pre-check (own short transaction): fail fast before calling user-service.
        UUID userId = refreshTokenService.validate(refreshToken).getUserId();

        // 2. Remote call, no transaction.
        UserDto user = userServiceClient.getUser(userId, null);

        // 3. Re-validate under a row lock and rotate atomically, so two concurrent refreshes
        //    with the same token cannot both succeed.
        return transactionTemplate.execute(status -> {
            RefreshToken current = refreshTokenService.validateForUpdate(refreshToken);
            Session session = sessionService.validateSession(current.getSession().getId());
            String accessToken = jwtService.generateToken(user, session.getId());
            RefreshToken rotated = refreshTokenService.rotate(current);
            return new LoginResponse(accessToken, rotated.getToken());
        });
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            RefreshToken rt = refreshTokenService.findByToken(refreshToken);
            refreshTokenService.revokeBySessionId(rt.getSession().getId());
            sessionService.closeSession(rt.getSession().getId());
        } catch (EntityNotFoundException | IllegalStateException e) {
            log.debug("Logout: token not found or already invalidated");
        }
    }
}
