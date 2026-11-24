package ru.saschoolsport.authz.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.repository.PermissionRepository;
import ru.saschoolsport.authz.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAndPermissionServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;

    private static UserPermission permission(long id, String resource, String action) {
        UserPermission p = new UserPermission(resource, action);
        p.setId(id);
        return p;
    }

    // ---- RoleService.addRolePermissions ----

    @Test
    void addRolePermissions_grantsKnownAndReportsUnknownIds() {
        RoleService service = new RoleService(roleRepository, permissionRepository);
        Role role = new Role("ROLE_X", "X", null);
        when(roleRepository.findActiveByIdWithPermissions(5L)).thenReturn(Optional.of(role));
        UserPermission read = permission(1L, "courses", "read");
        UserPermission edit = permission(2L, "courses", "update");
        Set<Long> requested = new LinkedHashSet<>(List.of(1L, 2L, 99L));
        when(permissionRepository.findAllById(requested)).thenReturn(List.of(read, edit));

        List<Long> notFound = service.addRolePermissions(5L, requested);

        assertThat(notFound).containsExactly(99L);
        assertThat(role.getPermissions()).containsExactlyInAnyOrder(read, edit);
        verify(roleRepository).save(role);
    }

    @Test
    void addRolePermissions_isIdempotentAndKeepsExistingGrants() {
        RoleService service = new RoleService(roleRepository, permissionRepository);
        Role role = new Role("ROLE_X", "X", null);
        UserPermission read = permission(1L, "courses", "read");
        UserPermission other = permission(3L, "users", "read");
        role.addPermission(other);
        role.addPermission(read);
        when(roleRepository.findActiveByIdWithPermissions(5L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(Set.of(1L))).thenReturn(List.of(read));

        List<Long> notFound = service.addRolePermissions(5L, Set.of(1L));

        assertThat(notFound).isEmpty();
        assertThat(role.getPermissions()).containsExactlyInAnyOrder(read, other);
    }

    @Test
    void addRolePermissions_unknownRoleFailsWithoutTouchingPermissions() {
        RoleService service = new RoleService(roleRepository, permissionRepository);
        when(roleRepository.findActiveByIdWithPermissions(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addRolePermissions(404L, Set.of(1L)))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(permissionRepository);
        verify(roleRepository, never()).save(any());
    }

    // ---- PermissionService.createPermission ----

    @Test
    void createPermission_buildsNameFromResourceAndAction() {
        PermissionService service = new PermissionService(permissionRepository);
        when(permissionRepository.findByName("courses:archive")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(UserPermission.class))).thenAnswer(i -> i.getArgument(0));

        UserPermission created = service.createPermission("courses", "archive", "Archive courses");

        assertThat(created.getName()).isEqualTo("courses:archive");
        assertThat(created.getDescription()).isEqualTo("Archive courses");
    }

    @Test
    void createPermission_duplicateIsConflictAndNotSaved() {
        PermissionService service = new PermissionService(permissionRepository);
        when(permissionRepository.findByName("courses:read"))
                .thenReturn(Optional.of(permission(1L, "courses", "read")));

        assertThatThrownBy(() -> service.createPermission("courses", "read", null))
                .isInstanceOf(EntityExistsException.class);

        verify(permissionRepository, never()).save(any());
    }
}
