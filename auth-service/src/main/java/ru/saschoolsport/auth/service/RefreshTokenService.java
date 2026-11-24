package ru.saschoolsport.auth.service;

import ru.saschoolsport.auth.config.ServiceProperties;
import ru.saschoolsport.auth.entity.RTStatus;
import ru.saschoolsport.auth.entity.RefreshToken;
import ru.saschoolsport.auth.entity.Session;
import ru.saschoolsport.auth.exception.RefreshTokenReuseException;
import ru.saschoolsport.auth.repository.RefreshTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ServiceProperties serviceProperties;

    @Transactional
    public RefreshToken create(UUID userId, Session session) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString());
        // TODO set Session Life check
        rt.setExpiresAt(Instant.now().plus(serviceProperties.getRefreshTokenTtl()));
        rt.setSession(session);
        return refreshTokenRepository.save(rt);
    }

    /**
     * Finds a token by its opaque value without any status/expiry checks.
     * Used by logout to revoke a session even if the token has already been rotated.
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));
    }

    /**
     * Validates that the token is ACTIVE and not expired.
     * Throws {@link EntityNotFoundException} if the token does not exist,
     * or {@link IllegalStateException} if it is not usable.
     */
    public RefreshToken validate(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));
        return requireUsable(rt);
    }

    /**
     * Same checks as {@link #validate}, but takes a row lock. Must run inside a transaction
     * that also performs the rotation, so concurrent use of one token is serialised.
     */
    @Transactional
    public RefreshToken validateForUpdate(String token) {
        RefreshToken rt = refreshTokenRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));
        return requireUsable(rt);
    }

    private RefreshToken requireUsable(RefreshToken rt) {
        if (rt.getStatus() == RTStatus.ROTATED) {
            throw new RefreshTokenReuseException(rt.getSession().getId());
        }
        if (rt.getStatus() != RTStatus.ACTIVE) {
            throw new IllegalStateException("Refresh token is not active: " + rt.getStatus());
        }
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token expired");
        }
        return rt;
    }

    @Transactional
    public RefreshToken rotate(RefreshToken oldRt) {
        oldRt.setStatus(RTStatus.ROTATED);
        refreshTokenRepository.save(oldRt);

        RefreshToken newRt = new RefreshToken();
        newRt.setUserId(oldRt.getUserId());
        newRt.setSession(oldRt.getSession());
        newRt.setToken(UUID.randomUUID().toString());
        // TODO set Session Life check
        newRt.setExpiresAt(Instant.now().plus(serviceProperties.getRefreshTokenTtl()));
        return refreshTokenRepository.save(newRt);
    }

    @Transactional
    public void revokeBySessionId(UUID sessionId) {
        refreshTokenRepository.revokeAllActiveBySessionId(sessionId);
    }
}
