package ru.saschoolsport.authz.service;

import ru.saschoolsport.authz.client.UserServiceClient;
import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.exception.ServiceUnavailableException;
import ru.saschoolsport.authz.entity.UserRole;
import ru.saschoolsport.authz.repository.RoleRepository;
import ru.saschoolsport.authz.repository.UserRoleRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserServiceClient userServiceClient;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Distinct ids of users holding any of the given roles. Roles are identified by
     * {@code code} (e.g. {@code ROLE_ADMIN}); unknown or deleted codes are ignored.
     */
    @Transactional(readOnly = true)
    public List<UUID> getUserIdsByRoles(List<String> roleCodes) {
        Set<UUID> userIds = new LinkedHashSet<>();
        for (String code : roleCodes) {
            roleRepository.findActiveIdByCode(code)
                    .ifPresent(roleId -> userIds.addAll(userRoleRepository.findUserIdsByRoleId(roleId)));
        }
        return new ArrayList<>(userIds);
    }

    /**
     * Assigns a role to a user on behalf of an administrator. Unlike {@link #assignRoleToUser}
     * this verifies that the user really exists in user-service, so ids cannot be invented.
     * The remote call is made outside any DB transaction.
     *
     * @throws EntityNotFoundException      role (or user) does not exist
     * @throws ServiceUnavailableException  user-service cannot be reached
     */
    public void assignRole(UUID userId, Long roleId) {
        roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
        try {
            userServiceClient.getUser(userId, null);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        assignRoleToUser(userId, roleId);
    }

    /**
     * Assigns a role to a user identified by UUID.
     * No Feign call — userId is already known (e.g. from a Kafka event).
     * DB unique constraint + exists-check = idempotent.
     */
    public void assignRoleToUser(UUID userId, Long roleId) {
        Role role = roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }

        try {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        } catch (DataIntegrityViolationException e) {
            log.debug("Role {} already assigned to user {} (concurrent insert), skipping", roleId, userId);
        }
    }

    /**
     * Resolves username → UUID via user-service, then delegates to assignRoleToUser.
     * Used by DataInitializer where only the username is known.
     * Feign call is intentionally outside any DB transaction.
     */
    public void assignRoleByUsername(String username, Long roleId) {
        var user = userServiceClient.getUser(null, username);
        assignRoleToUser(user.getId(), roleId);
    }

    /**
     * Revokes a role from a user. No-op if the assignment does not exist.
     * No Feign call — userId is already known.
     */
    @Transactional
    public void revokeRole(UUID userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }
}
