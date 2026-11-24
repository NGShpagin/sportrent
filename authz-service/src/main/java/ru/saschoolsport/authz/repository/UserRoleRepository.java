package ru.saschoolsport.authz.repository;

import ru.saschoolsport.authz.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, Long>  {
    boolean existsByUserIdAndRoleId(UUID userId, Long roleId);

    void deleteByUserIdAndRoleId(UUID userId, Long roleId);

    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.role.id = :roleId")
    List<UUID> findUserIdsByRoleId(@Param("roleId") Long roleId);
}
