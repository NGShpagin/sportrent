package ru.saschoolsport.auth.repository;

import ru.saschoolsport.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /** Row-locked lookup; must be called inside a transaction (used for atomic rotation). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.status = ru.saschoolsport.auth.entity.RTStatus.REVOKED
         where rt.userId = :userId
           and rt.status = ru.saschoolsport.auth.entity.RTStatus.ACTIVE
    """)
    void revokeAllActiveByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.status = ru.saschoolsport.auth.entity.RTStatus.REVOKED
         where rt.session.id = :sessionId
           and rt.status = ru.saschoolsport.auth.entity.RTStatus.ACTIVE
    """)
    void revokeAllActiveBySessionId(@Param("sessionId") UUID sessionId);
}
