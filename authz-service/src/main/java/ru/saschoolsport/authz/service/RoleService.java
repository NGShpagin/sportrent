package ru.saschoolsport.authz.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.saschoolsport.authz.dto.UpdateRoleRequestDto;
import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.repository.PermissionRepository;
import ru.saschoolsport.authz.repository.RoleRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "name", "code", "createdAt", "updatedAt"
    );

    @Transactional
    public Role createRole(String roleCode, String roleName, String description) {
        log.info("Creating role: {}", roleCode);
        if (roleRepository.existsByCodeAndDeletedAtIsNull(roleCode)) {
            throw new EntityExistsException("Role already exists with code: " + roleCode);
        }
        Role role = new Role(roleCode, roleName, description);
        Role saved = roleRepository.save(role);
        log.info("Role created with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Creates the role if no active role with the given code exists; otherwise returns the existing one.
     */
    @Transactional
    public Role findOrCreate(String code, String name, String description) {
        return roleRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseGet(() -> createRole(code, name, description));
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findActiveByIdWithPermissions(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
    }

    /**
     * Paginated role list.
     *
     * @param userId optional filter: when set, only roles assigned to that user
     */
    @Transactional(readOnly = true)
    public Page<Role> getRoles(String sortBy, String sortDir, int page, int size, UUID userId) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir)
                .orElseThrow(() -> new IllegalArgumentException("sort_dir must be 'asc' or 'desc'"));
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1 (1-indexed)");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, resolveSortProperty(sortBy)));
        if (userId != null) {
            return roleRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
        }
        return roleRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public List<Role> getUserRoles(UUID userId) {
        return roleRepository.findActiveByUserIdWithPermissions(userId);
    }

    @Transactional
    public void deleteRoleById(Long id) {
        Role role = roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
        role.setDeletedAt(Instant.now());
        roleRepository.save(role);
        log.info("Role soft-deleted: {}", id);
    }

    /**
     * Replaces the role's permissions and renames/redescribes the role.
     * Returns IDs of any permission IDs that were not found.
     */
    @Transactional
    public List<Long> updateRole(Long roleId, UpdateRoleRequestDto request) {
        Role role = roleRepository.findActiveByIdWithPermissions(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        List<Long> permissionIds = request.getPermissions();
        List<UserPermission> found = permissionRepository.findAllById(permissionIds);

        List<Long> notFoundIds = new ArrayList<>(permissionIds);
        found.forEach(p -> notFoundIds.remove(p.getId()));

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.getPermissions().clear();
        role.getPermissions().addAll(found);

        roleRepository.save(role);
        return notFoundIds;
    }

    /**
     * Grants the given permissions to the role (existing grants are kept; idempotent).
     *
     * @return ids from {@code permissionIds} that do not exist and were therefore not granted
     */
    @Transactional
    public List<Long> addRolePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findActiveByIdWithPermissions(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        List<UserPermission> found = permissionRepository.findAllById(permissionIds);
        role.getPermissions().addAll(found);
        roleRepository.save(role);

        Set<Long> foundIds = found.stream().map(UserPermission::getId).collect(java.util.stream.Collectors.toSet());
        return permissionIds.stream().filter(id -> !foundIds.contains(id)).toList();
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "name";
        }
        if (ALLOWED_SORT_PROPERTIES.contains(sortBy)) {
            return sortBy;
        }
        String camelCase = toCamelCase(sortBy);
        if (ALLOWED_SORT_PROPERTIES.contains(camelCase)) {
            return camelCase;
        }
        throw new IllegalArgumentException(
                "Invalid sort property: " + sortBy + ". Allowed: " + ALLOWED_SORT_PROPERTIES);
    }

    private static String toCamelCase(String snakeCase) {
        if (!snakeCase.contains("_")) {
            return snakeCase;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : snakeCase.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
