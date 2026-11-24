package ru.saschoolsport.auth.repository;

import ru.saschoolsport.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Session s
           set s.status = ru.saschoolsport.auth.entity.SessionStatus.REVOKED
         where s.userId = :userId
           and s.status = ru.saschoolsport.auth.entity.SessionStatus.ACTIVE
    """)
    void revokeAllActiveByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Session s
           set s.status = ru.saschoolsport.auth.entity.SessionStatus.REVOKED
         where s.id = :sessionId
           and s.status = ru.saschoolsport.auth.entity.SessionStatus.ACTIVE
    """)
    void revokeActiveById(@Param("sessionId") UUID sessionId);
}
