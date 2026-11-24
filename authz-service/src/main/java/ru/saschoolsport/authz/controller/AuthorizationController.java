package ru.saschoolsport.authz.controller;

import ru.saschoolsport.authz.dto.*;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.service.PermissionService;
import ru.saschoolsport.authz.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.saschoolsport.authz.service.UserRoleService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthorizationController {

    private final PermissionService permissionService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;

    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RoleDto.fromEntity(roleService.createRole(
                        request.getCode(),
                        request.getName(),
                        request.getDescription()
                )));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<UpdateRoleResponseDto> updateRole(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateRoleRequestDto request
    ) {
        List<Long> notFoundIds = roleService.updateRole(id, request);
        return ResponseEntity.ok(new UpdateRoleResponseDto(notFoundIds));
    }

    @GetMapping("/roles")
    public ResponseEntity<PagedRoleResponse> getRoles(@RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
                                                      @RequestParam(value = "sort_dir", defaultValue = "asc") String sortDir,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "size", defaultValue = "10") int size,
                                                      @RequestParam(required = false, name = "user_id") UUID userId) {
        return ResponseEntity.ok(PagedRoleResponse.from(roleService.getRoles(sortBy, sortDir, page, size, userId)));
    }

    @GetMapping("/internal/roles")
    public ResponseEntity<List<RoleDto>> getUserRoles(@RequestParam(name = "user_id") UUID userId) {
        return ResponseEntity.ok(RoleDto.fromEntities(roleService.getUserRoles(userId)));
    }

    @PostMapping("/permissions")
    public ResponseEntity<UserPermissionDto> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        UserPermission created = permissionService.createPermission(
                request.getResource(), request.getAction(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserPermissionDto.fromEntity(created));
    }

    @PostMapping("/roles/{id}/permissions")
    public ResponseEntity<AddRolePermissionsResponse> addRolePermissions(
            @PathVariable("id") Long roleId,
            @Valid @RequestBody AddRolePermissionsRequest request
    ) {
        Set<Long> requested = new LinkedHashSet<>(request.getPermissions());
        List<Long> notFound = roleService.addRolePermissions(roleId, requested);
        List<Long> granted = requested.stream().filter(id -> !notFound.contains(id)).toList();
        return ResponseEntity.ok(new AddRolePermissionsResponse(notFound, granted));
    }

    /** Idempotent: assigning an already assigned role succeeds with 204. */
    @PutMapping("/roles/{roleId}/users/{userId}")
    public ResponseEntity<Void> assignRole(@PathVariable("roleId") Long roleId,
                                           @PathVariable("userId") UUID userId) {
        userRoleService.assignRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    /** Idempotent: revoking a role the user does not have succeeds with 204. */
    @DeleteMapping("/roles/{roleId}/users/{userId}")
    public ResponseEntity<Void> revokeRole(@PathVariable("roleId") Long roleId,
                                           @PathVariable("userId") UUID userId) {
        userRoleService.revokeRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<UserPermissionDto>> getPermissions(
            @RequestParam(required = false, name = "user_id") UUID userId,
            @RequestParam(required = false, name = "role_id") Long roleId,
            @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(UserPermissionDto.fromEntities(permissionService.getPermissions(userId, roleId, sortBy, sortDir)));
    }

    @GetMapping("/me/permissions")
    public ResponseEntity<List<UserPermissionDto>> getCurrentUserPermissions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(UserPermissionDto.fromEntities(permissionService.getPermissions(userId, null, sortBy, sortDir)));
    }

    @GetMapping("/internal/permissions")
    public ResponseEntity<List<String>> getUserPermissionNames(@RequestParam(value = "user_id") UUID userId) {
        return ResponseEntity.ok(permissionService.getPermissions(userId, null, "name", "asc")
                .stream()
                .map(UserPermission::getName)
                .toList());
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(RoleDto.fromEntity(roleService.getRoleById(id)));
    }

    @GetMapping("/internal/roles/users")
    public ResponseEntity<List<UUID>> getUserIdsByRoles(@RequestParam(value = "role_names") List<String> roles) {
        return ResponseEntity.ok(userRoleService.getUserIdsByRoles(roles));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRoleById(@PathVariable("id") Long id) {
        roleService.deleteRoleById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/permissions/check")
    public ResponseEntity<PermissionCheckResponseDto> hasPermission(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("permission") String permission
    ) {
        return ResponseEntity.ok(new PermissionCheckResponseDto(permissionService.hasPermission(userId, permission)));
    }
}
