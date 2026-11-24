package ru.saschoolsport.authz.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.saschoolsport.authz.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    Optional<Role> findByIdAndDeletedAtIsNull(Long id);

    Page<Role> findAllByDeletedAtIsNull(Pageable pageable);

    @Query(
            value = """
                    SELECT r FROM Role r
                    JOIN UserRole ur ON ur.role.id = r.id
                    WHERE ur.userId = :userId
                      AND r.deletedAt IS NULL
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM Role r
                    JOIN UserRole ur ON ur.role.id = r.id
                    WHERE ur.userId = :userId
                      AND r.deletedAt IS NULL
                    """)
    Page<Role> findAllByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId, Pageable pageable);

    /**
     * All active roles with permissions pre-fetched (avoids N+1).
     */
    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.deletedAt IS NULL
            ORDER BY r.createdAt
            """)
    List<Role> findAllActiveWithPermissions();

    @Query("""
            SELECT r.id FROM Role r
            WHERE r.code = :code
              AND r.deletedAt IS NULL
            """)
    Optional<Long> findActiveIdByCode(@Param("code") String code);

    /**
     * Active roles for a user with permissions pre-fetched (avoids N+1).
     */
    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            JOIN UserRole ur ON r.id = ur.role.id
            WHERE ur.userId = :userId
              AND r.deletedAt IS NULL
            """)
    List<Role> findActiveByUserIdWithPermissions(@Param("userId") UUID userId);

    /**
     * Single active role with permissions pre-fetched.
     */
    @Query("""
            SELECT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.id = :id
              AND r.deletedAt IS NULL
            """)
    Optional<Role> findActiveByIdWithPermissions(@Param("id") Long id);
}
