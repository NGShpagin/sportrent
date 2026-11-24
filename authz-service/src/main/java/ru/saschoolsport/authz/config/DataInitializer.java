package ru.saschoolsport.authz.config;

import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.exception.ServiceUnavailableException;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.service.PermissionService;
import ru.saschoolsport.authz.service.RoleService;
import ru.saschoolsport.authz.service.UserRoleService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int ADMIN_ASSIGN_RETRIES = 20;
    private static final long ADMIN_ASSIGN_DELAY_MS = 3_000;

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;

    @Override
    public void run(String... args) {
        // 1. Seed base permissions (idempotent)
        UserPermission userRead      = permissionService.findOrCreate("users",   "read",    "View users");
        UserPermission userCreate    = permissionService.findOrCreate("users",   "create",  "Create users");
        UserPermission userUpdate    = permissionService.findOrCreate("users",   "update",  "Update users");
        UserPermission userDelete    = permissionService.findOrCreate("users",   "delete",  "Delete users");
        UserPermission rolesManage   = permissionService.findOrCreate("roles",   "manage",  "Manage roles");

        // 2. Seed ADMIN role (idempotent)
        Role adminRole = roleService.findOrCreate("ROLE_ADMIN", "Administrator", "Full system access");
        Role workerRole = roleService.findOrCreate("ROLE_WORKER", "Worker", "WORKER system access");
        Role userRole = roleService.findOrCreate("ROLE_USER", "User", "User system access");

        // 3. Ensure admin role has all permissions
        roleService.addRolePermissions(adminRole.getId(), Set.of(
                userRead.getId(),
                userCreate.getId(),
                userUpdate.getId(),
                userDelete.getId(),
                rolesManage.getId()
        ));

        roleService.addRolePermissions(workerRole.getId(), Set.of(
                userRead.getId()
        ));

        roleService.addRolePermissions(userRole.getId(), Set.of(
                userRead.getId()
        ));

        // 4. Assign admin role — retry because user-service may still be initializing
        assignAdminRoleWithRetry(adminRole.getId());
    }

    private void assignAdminRoleWithRetry(Long adminRoleId) {
        for (int attempt = 1; attempt <= ADMIN_ASSIGN_RETRIES; attempt++) {
            try {
                userRoleService.assignRoleByUsername("admin", adminRoleId);
                log.info("Admin role assigned to user 'admin'");
                return;
            } catch (FeignException.NotFound e) {
                log.info("Attempt {}/{}: admin user not yet available in user-service, retrying in {}ms...",
                        attempt, ADMIN_ASSIGN_RETRIES, ADMIN_ASSIGN_DELAY_MS);
            } catch (FeignException e) {
                log.warn("Attempt {}/{}: user-service unreachable ({}), retrying in {}ms...",
                        attempt, ADMIN_ASSIGN_RETRIES, e.status(), ADMIN_ASSIGN_DELAY_MS);
            } catch (ServiceUnavailableException e) {
                log.info("Attempt {}/{}: user-service not registered/reachable yet, retrying in {}ms...",
                        attempt, ADMIN_ASSIGN_RETRIES, ADMIN_ASSIGN_DELAY_MS);
            } catch (IllegalStateException e) {
                log.warn("Attempt {}/{}: user-service unavailable ({}), retrying in {}ms...",
                        attempt, ADMIN_ASSIGN_RETRIES, e.getMessage(), ADMIN_ASSIGN_DELAY_MS);
            }

            if (attempt < ADMIN_ASSIGN_RETRIES) {
                try {
                    Thread.sleep(ADMIN_ASSIGN_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Admin role assignment interrupted");
                    return;
                }
            }
        }
        log.warn("Could not assign admin role after {} attempts — will need manual assignment", ADMIN_ASSIGN_RETRIES);
    }
}
