package ru.saschoolsport.auth.service;

import ru.saschoolsport.auth.config.ServiceProperties;
import ru.saschoolsport.auth.entity.Session;
import ru.saschoolsport.auth.entity.SessionStatus;
import ru.saschoolsport.auth.repository.RefreshTokenRepository;
import ru.saschoolsport.auth.repository.SessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ServiceProperties serviceProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Session createSession(UUID userId) {
        Session session = new Session();
        session.setUserId(userId);
        session.setExpiresAt(Instant.now().plus(serviceProperties.getSessionTtl()));
        return sessionRepository.save(session);
    }

    /**
     * Validates that the session exists, is ACTIVE and not expired.
     * Throws {@link EntityNotFoundException} if not found,
     * or {@link IllegalStateException} if inactive/expired.
     */
    @Transactional(readOnly = true)
    public Session validateSession(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session is not active: " + session.getStatus());
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Session expired");
        }
        return session;
    }

    @Transactional
    public void closeSession(UUID sessionId) {
        sessionRepository.revokeActiveById(sessionId);
        refreshTokenRepository.revokeAllActiveBySessionId(sessionId);
    }

    @Transactional
    public void revokeAllByUserId(UUID userId) {
        sessionRepository.revokeAllActiveByUserId(userId);
        refreshTokenRepository.revokeAllActiveByUserId(userId);
    }
}
