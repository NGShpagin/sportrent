package ru.saschoolsport.authz.repository;

import ru.saschoolsport.authz.entity.UserPermission;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<UserPermission, Long> {
    Optional<UserPermission> findByName(String name);

    @Query("SELECT p FROM UserPermission p WHERE p.resource = :resource")
    List<UserPermission> findByResource(@Param("resource") String resource);

    @Query("""
            SELECT DISTINCT p
            FROM UserRole ur
            JOIN ur.role r
            JOIN r.permissions p
            WHERE ur.userId = :userId
              AND r.deletedAt is null
            """)
    List<UserPermission> findPermissionsByUserId(@Param("userId") UUID userId, Sort sort);

    @Query("""
            SELECT DISTINCT p
            FROM Role r
            JOIN r.permissions p
            WHERE r.id = :roleId
              AND r.deletedAt is null
            """)
    List<UserPermission> findPermissionsByRoleId(@Param("roleId") Long roleId, Sort sort);

    @Query("""
            SELECT COUNT(p) > 0
            FROM UserRole ur
            JOIN ur.role r
            JOIN r.permissions p
            WHERE ur.userId = :userId
              AND r.deletedAt IS NULL
              AND p.name = :permission
            """)
    boolean existsUserPermission(@Param("userId") UUID userId,
                                 @Param("permission") String permission);
}
