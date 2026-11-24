package ru.saschoolsport.auth.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import ru.saschoolsport.auth.client.UserServiceClient;
import ru.saschoolsport.auth.dto.LoginRequestDto;
import ru.saschoolsport.auth.dto.LoginResponse;
import ru.saschoolsport.auth.dto.UserDto;
import ru.saschoolsport.auth.entity.RefreshToken;
import ru.saschoolsport.auth.entity.Session;
import ru.saschoolsport.auth.exception.RefreshTokenReuseException;
import ru.saschoolsport.auth.exception.ServiceUnavailableException;
import ru.saschoolsport.auth.support.FeignTestSupport;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The key property under test: user-service is never called while a DB transaction is open.
 * A tracking transaction manager records whether a transaction is currently active.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock JwtService jwtService;
    @Mock UserServiceClient userServiceClient;
    @Mock RefreshTokenService refreshTokenService;
    @Mock SessionService sessionService;

    final AtomicBoolean inTransaction = new AtomicBoolean();
    AuthService service;

    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    UserDto user;
    Session session;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager tracking = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                inTransaction.set(true);
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                inTransaction.set(false);
            }

            @Override
            public void rollback(TransactionStatus status) {
                inTransaction.set(false);
            }
        };
        service = new AuthService(jwtService, userServiceClient, refreshTokenService, sessionService,
                new TransactionTemplate(tracking));

        user = UserDto.builder().id(userId).username("alice").build();
        session = new Session();
        session.setId(sessionId);
    }

    private RefreshToken refreshToken(String value) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(value);
        rt.setSession(session);
        return rt;
    }

    // ---- authenticate ----

    @Test
    void authenticate_callsUserServiceOutsideTransactionThenCreatesSessionAndTokensInside() {
        when(userServiceClient.authenticate(any(LoginRequestDto.class))).thenAnswer(i -> {
            assertThat(inTransaction).as("user-service call must not run inside a transaction").isFalse();
            return user;
        });
        when(sessionService.createSession(userId)).thenAnswer(i -> {
            assertThat(inTransaction).as("session creation must be transactional").isTrue();
            return session;
        });
        when(jwtService.generateToken(user, sessionId)).thenReturn("access");
        when(refreshTokenService.create(userId, session)).thenReturn(refreshToken("refresh"));

        LoginResponse response = service.authenticate("alice", "pw");

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(inTransaction).isFalse();
    }

    @Test
    void authenticate_wrongCredentialsAllLookTheSame() {
        for (FeignException failure : new FeignException[]{
                FeignTestSupport.unauthorized(), FeignTestSupport.forbidden(), FeignTestSupport.notFound()}) {
            // doThrow: re-stubbing with when(...) would invoke the previous stub and throw
            doThrow(failure).when(userServiceClient).authenticate(any());

            assertThatThrownBy(() -> service.authenticate("alice", "bad"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");
        }
        verifyNoInteractions(sessionService, refreshTokenService, jwtService);
    }

    @Test
    void authenticate_downstreamServerErrorPropagatesAndCreatesNothing() {
        when(userServiceClient.authenticate(any())).thenThrow(FeignTestSupport.serverError());

        assertThatThrownBy(() -> service.authenticate("alice", "pw")).isInstanceOf(FeignException.class);

        verifyNoInteractions(sessionService, refreshTokenService, jwtService);
    }

    // ---- refresh ----

    @Test
    void refresh_validatesThenCallsUserServiceOutsideTransactionThenRotatesUnderLock() {
        RefreshToken current = refreshToken("old");
        RefreshToken rotated = refreshToken("new");
        when(refreshTokenService.validate("old")).thenAnswer(i -> {
            assertThat(inTransaction).isFalse();
            return current;
        });
        when(userServiceClient.getUser(userId, null)).thenAnswer(i -> {
            assertThat(inTransaction).as("user-service call must not run inside a transaction").isFalse();
            return user;
        });
        when(refreshTokenService.validateForUpdate("old")).thenAnswer(i -> {
            assertThat(inTransaction).as("locked re-validation must be inside the transaction").isTrue();
            return current;
        });
        when(sessionService.validateSession(sessionId)).thenReturn(session);
        when(jwtService.generateToken(user, sessionId)).thenReturn("access2");
        when(refreshTokenService.rotate(current)).thenReturn(rotated);

        LoginResponse response = service.refresh("old");

        assertThat(response.getAccessToken()).isEqualTo("access2");
        assertThat(response.getRefreshToken()).isEqualTo("new");
        InOrder order = inOrder(refreshTokenService, userServiceClient);
        order.verify(refreshTokenService).validate("old");
        order.verify(userServiceClient).getUser(userId, null);
        order.verify(refreshTokenService).validateForUpdate("old");
        order.verify(refreshTokenService).rotate(current);
    }

    @Test
    void refresh_invalidTokenFailsFastWithoutCallingUserService() {
        when(refreshTokenService.validate("nope")).thenThrow(new EntityNotFoundException("Refresh token not found"));

        assertThatThrownBy(() -> service.refresh("nope")).isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(userServiceClient, jwtService);
        verify(refreshTokenService, never()).validateForUpdate(any());
    }

    @Test
    void refresh_tokenUsedConcurrentlyIsRejectedUnderLockAndNotRotated() {
        // pre-check passes, but by the time the row lock is taken another request already rotated it
        when(refreshTokenService.validate("old")).thenReturn(refreshToken("old"));
        when(userServiceClient.getUser(userId, null)).thenReturn(user);
        when(refreshTokenService.validateForUpdate("old"))
                .thenThrow(new RefreshTokenReuseException(sessionId));
        doAnswer(i -> {
            assertThat(inTransaction).as("session must be closed after the failed tx is rolled back").isFalse();
            return null;
        }).when(sessionService).closeSession(sessionId);

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenService, never()).rotate(any());
        verify(sessionService).closeSession(sessionId);
        verifyNoInteractions(jwtService);
        assertThat(inTransaction).as("transaction must be closed after failure").isFalse();
    }

    @Test
    void refresh_reusedRotatedTokenClosesTheWholeSessionAndCallsNobody() {
        when(refreshTokenService.validate("old")).thenThrow(new RefreshTokenReuseException(sessionId));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(RefreshTokenReuseException.class);

        verify(sessionService).closeSession(sessionId);
        verifyNoInteractions(userServiceClient, jwtService);
        verify(refreshTokenService, never()).rotate(any());
    }

    @Test
    void refresh_otherInvalidTokensDoNotCloseAnySession() {
        for (RuntimeException failure : new RuntimeException[]{
                new EntityNotFoundException("Refresh token not found"),
                new IllegalStateException("Refresh token is not active: REVOKED"),
                new IllegalStateException("Refresh token expired")}) {
            doThrow(failure).when(refreshTokenService).validate("t");

            assertThatThrownBy(() -> service.refresh("t")).isSameAs(failure);
        }
        verifyNoInteractions(sessionService);
    }

    @Test
    void refresh_userServiceDownDoesNotOpenATransactionOrRotate() {
        when(refreshTokenService.validate("old")).thenReturn(refreshToken("old"));
        when(userServiceClient.getUser(userId, null)).thenThrow(new ServiceUnavailableException("down", null));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(ServiceUnavailableException.class);

        verify(refreshTokenService, never()).validateForUpdate(any());
        verify(refreshTokenService, never()).rotate(any());
    }
}
