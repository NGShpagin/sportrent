package ru.saschoolsport.authz.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.test.context.ActiveProfiles;
import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.entity.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the real JPQL against H2, so query typos fail here instead of at application startup. */
@DataJpaTest
@ActiveProfiles("test")
class AuthzRepositoriesTest {

    @Autowired TestEntityManager em;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRoleRepository userRoleRepository;
    @Autowired PermissionRepository permissionRepository;

    UserPermission usersRead, coursesRead;
    Role active, deleted;
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();

    @BeforeEach
    void data() {
        usersRead = em.persist(new UserPermission("users", "read"));
        coursesRead = em.persist(new UserPermission("courses", "read"));

        active = new Role("ROLE_ACTIVE", "Active", null);
        active.addPermission(usersRead);
        active.addPermission(coursesRead);
        em.persist(active);

        deleted = new Role("ROLE_DELETED", "Deleted", null);
        deleted.addPermission(usersRead);
        deleted.setDeletedAt(Instant.now());
        em.persist(deleted);

        assign(alice, active);
        assign(bob, active);
        assign(alice, deleted);
        em.flush();
        em.clear();
    }

    private void assign(UUID user, Role role) {
        UserRole ur = new UserRole();
        ur.setUserId(user);
        ur.setRole(role);
        em.persist(ur);
    }

    @Test
    void findActiveIdByCode_findsOnlyActiveRoles() {
        assertThat(roleRepository.findActiveIdByCode("ROLE_ACTIVE")).contains(active.getId());
        assertThat(roleRepository.findActiveIdByCode("ROLE_DELETED")).isEmpty();
        assertThat(roleRepository.findActiveIdByCode("ROLE_NOPE")).isEmpty();
    }

    @Test
    void findUserIdsByRoleId_returnsUserIdsNotEntities() {
        List<UUID> ids = userRoleRepository.findUserIdsByRoleId(active.getId());

        assertThat(ids).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void existsUserPermission_ignoresPermissionsFromDeletedRoles() {
        // alice holds users:read through both roles, bob only through the active one;
        // courses:read exists only via the active role
        assertThat(permissionRepository.existsUserPermission(alice, "users:read")).isTrue();
        assertThat(permissionRepository.existsUserPermission(bob, "courses:read")).isTrue();
        assertThat(permissionRepository.existsUserPermission(bob, "roles:manage")).isFalse();
        assertThat(permissionRepository.existsUserPermission(UUID.randomUUID(), "users:read")).isFalse();
    }

    @Test
    void findPermissionsByUserId_isDistinctAndSorted() {
        List<UserPermission> perms = permissionRepository.findPermissionsByUserId(
                alice, JpaSort.unsafe(Sort.Direction.DESC, "p.name"));

        assertThat(perms).extracting(UserPermission::getName).containsExactly("users:read", "courses:read");
    }

    @Test
    void findActiveByUserIdWithPermissions_fetchesPermissionsAndSkipsDeletedRoles() {
        List<Role> roles = roleRepository.findActiveByUserIdWithPermissions(alice);

        assertThat(roles).extracting(Role::getCode).containsExactly("ROLE_ACTIVE");
        // permissions are usable outside a session => they were fetched by the query
        assertThat(roles.get(0).getPermissions()).hasSize(2);
    }

    @Test
    void deleteByUserIdAndRoleId_removesOnlyThatAssignment() {
        userRoleRepository.deleteByUserIdAndRoleId(alice, active.getId());
        em.flush();

        assertThat(userRoleRepository.existsByUserIdAndRoleId(alice, active.getId())).isFalse();
        assertThat(userRoleRepository.existsByUserIdAndRoleId(bob, active.getId())).isTrue();
        assertThat(userRoleRepository.existsByUserIdAndRoleId(alice, deleted.getId())).isTrue();
    }
}
