package ru.saschoolsport.authz.service;

import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.repository.PermissionRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("name", "createdAt");

    @Transactional(readOnly = true)
    public List<UserPermission> getPermissions(UUID userId, Long roleId, String sortBy, String sortDir) {
        if (userId != null && roleId != null) {
            throw new IllegalArgumentException("Only one of user_id or role_id may be provided");
        }

        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir)
                .orElseThrow(() -> new IllegalArgumentException("sort_dir must be 'asc' or 'desc'"));
        String sortProperty = resolveSortProperty(sortBy);

        if (userId == null && roleId == null) {
            return permissionRepository.findAll(Sort.by(direction, sortProperty));
        }

        Sort sort = JpaSort.unsafe(direction, "p." + sortProperty);
        if (userId != null) {
            return permissionRepository.findPermissionsByUserId(userId, sort);
        }
        return permissionRepository.findPermissionsByRoleId(roleId, sort);
    }

    @Transactional
    public UserPermission createPermission(String resource, String action, String description) {
        String name = resource + ":" + action;
        if (permissionRepository.findByName(name).isPresent()) {
            throw new EntityExistsException("Permission already exists: " + name);
        }
        UserPermission permission = new UserPermission(resource, action, description);
        UserPermission saved = permissionRepository.save(permission);
        log.info("Created permission: {}", saved.getName());
        return saved;
    }

    /**
     * Returns an existing permission with the given name, or creates it.
     * Safe to call on every startup — idempotent.
     */
    @Transactional
    public UserPermission findOrCreate(String resource, String action, String description) {
        String name = resource + ":" + action;
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    UserPermission p = new UserPermission(resource, action, description);
                    UserPermission saved = permissionRepository.save(p);
                    log.info("Created permission: {}", saved.getName());
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String permission) {
        return permissionRepository.existsUserPermission(userId, permission);
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
        if (!snakeCase.contains("_")) return snakeCase;
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
