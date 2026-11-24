package ru.saschoolsport.auth.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.saschoolsport.auth.config.ServiceProperties;
import ru.saschoolsport.auth.entity.RTStatus;
import ru.saschoolsport.auth.entity.RefreshToken;
import ru.saschoolsport.auth.entity.Session;
import ru.saschoolsport.auth.exception.RefreshTokenReuseException;
import ru.saschoolsport.auth.repository.RefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository repository;
    RefreshTokenService service;

    @BeforeEach
    void setUp() {
        ServiceProperties properties = new ServiceProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        service = new RefreshTokenService(repository, properties);
    }

    private static RefreshToken token(RTStatus status, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setToken("t");
        rt.setStatus(status);
        rt.setExpiresAt(expiresAt);
        rt.setUserId(UUID.randomUUID());
        Session session = new Session();
        session.setId(UUID.randomUUID());
        rt.setSession(session);
        return rt;
    }

    @Test
    void validate_acceptsActiveUnexpiredToken() {
        RefreshToken rt = token(RTStatus.ACTIVE, Instant.now().plusSeconds(60));
        when(repository.findByToken("t")).thenReturn(Optional.of(rt));

        assertThat(service.validate("t")).isSameAs(rt);
    }

    @Test
    void validate_rejectsUnknownToken() {
        when(repository.findByToken("t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("t")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void validate_rejectsRevokedAndExpiredTokensWithoutFlaggingReuse() {
        for (RefreshToken rt : new RefreshToken[]{
                token(RTStatus.REVOKED, Instant.now().plusSeconds(60)),
                token(RTStatus.ACTIVE, Instant.now().minusSeconds(1))}) {
            when(repository.findByToken("t")).thenReturn(Optional.of(rt));

            assertThatThrownBy(() -> service.validate("t"))
                    .isInstanceOf(IllegalStateException.class)
                    .isNotInstanceOf(RefreshTokenReuseException.class);
        }
    }

    @Test
    void validate_rotatedTokenIsReportedAsReuseOfItsSession() {
        RefreshToken rotated = token(RTStatus.ROTATED, Instant.now().plusSeconds(60));
        when(repository.findByToken("t")).thenReturn(Optional.of(rotated));

        assertThatThrownBy(() -> service.validate("t"))
                .isInstanceOf(RefreshTokenReuseException.class)
                .extracting(e -> ((RefreshTokenReuseException) e).getSessionId())
                .isEqualTo(rotated.getSession().getId());
    }

    @Test
    void validateForUpdate_usesTheLockingQueryAndAppliesTheSameChecks() {
        RefreshToken rotated = token(RTStatus.ROTATED, Instant.now().plusSeconds(60));
        when(repository.findByTokenForUpdate("t")).thenReturn(Optional.of(rotated));

        assertThatThrownBy(() -> service.validateForUpdate("t")).isInstanceOf(RefreshTokenReuseException.class);

        verify(repository, never()).findByToken(any());
    }

    @Test
    void rotate_marksOldRotatedAndIssuesNewTokenForSameUserAndSession() {
        RefreshToken old = token(RTStatus.ACTIVE, Instant.now().plusSeconds(60));
        old.setToken("old");
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken fresh = service.rotate(old);

        assertThat(old.getStatus()).isEqualTo(RTStatus.ROTATED);
        assertThat(fresh.getToken()).isNotEqualTo("old").isNotBlank();
        assertThat(fresh.getStatus()).isEqualTo(RTStatus.ACTIVE);
        assertThat(fresh.getUserId()).isEqualTo(old.getUserId());
        assertThat(fresh.getSession()).isSameAs(old.getSession());
        assertThat(fresh.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }
}
