package ru.saschoolsport.authz.controller;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.saschoolsport.authz.entity.UserPermission;
import ru.saschoolsport.authz.exception.RestExceptionHandler;
import ru.saschoolsport.authz.exception.ServiceUnavailableException;
import ru.saschoolsport.authz.service.PermissionService;
import ru.saschoolsport.authz.service.RoleService;
import ru.saschoolsport.authz.service.UserRoleService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthorizationControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock PermissionService permissionService;
    @Mock RoleService roleService;
    @Mock UserRoleService userRoleService;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new AuthorizationController(permissionService, roleService, userRoleService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    // ---- POST /permissions ----

    @Test
    void createPermission_returns201WithBody() throws Exception {
        UserPermission created = new UserPermission("courses", "archive", "Archive courses");
        created.setId(42L);
        when(permissionService.createPermission("courses", "archive", "Archive courses")).thenReturn(created);

        mvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resource":"courses","action":"archive","description":"Archive courses"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("courses:archive"));
    }

    @Test
    void createPermission_duplicateIs409() throws Exception {
        when(permissionService.createPermission(any(), any(), any()))
                .thenThrow(new EntityExistsException("Permission already exists: courses:read"));

        mvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resource\":\"courses\",\"action\":\"read\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Permission already exists: courses:read"));
    }

    @Test
    void createPermission_rejectsInvalidInputWithoutCallingService() throws Exception {
        for (String body : List.of(
                "{\"resource\":\"\",\"action\":\"read\"}",              // blank resource
                "{\"resource\":\"courses\"}",                           // missing action
                "{\"resource\":\"Courses\",\"action\":\"read\"}",       // uppercase
                "{\"resource\":\"courses:x\",\"action\":\"read\"}",     // ':' would break "resource:action"
                "{\"resource\":\"courses\",\"action\":\"" + "a".repeat(21) + "\"}")) { // too long for column
            mvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(permissionService);
    }

    @Test
    void malformedJsonIs400NotAnInternalError() throws Exception {
        mvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /roles/{id}/permissions ----

    @Test
    void addRolePermissions_reportsGrantedAndUnknownIds() throws Exception {
        when(roleService.addRolePermissions(eq(5L), eq(Set.of(1L, 2L, 99L)))).thenReturn(List.of(99L));

        mvc.perform(post("/roles/5/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[1,2,99]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successPermissions[0]").value(1))
                .andExpect(jsonPath("$.successPermissions[1]").value(2))
                .andExpect(jsonPath("$.successPermissions.length()").value(2))
                .andExpect(jsonPath("$.errorPermissions[0]").value(99));
    }

    @Test
    void addRolePermissions_unknownRoleIs404() throws Exception {
        when(roleService.addRolePermissions(eq(404L), any())).thenThrow(new EntityNotFoundException("Role not found: 404"));

        mvc.perform(post("/roles/404/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[1]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addRolePermissions_emptyOrNullListIs400() throws Exception {
        for (String body : List.of("{\"permissions\":[]}", "{}", "{\"permissions\":[1,null]}")) {
            mvc.perform(post("/roles/5/permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(roleService);
    }

    // ---- PUT/DELETE /roles/{roleId}/users/{userId} ----

    @Test
    void assignRole_returns204() throws Exception {
        mvc.perform(put("/roles/7/users/" + USER_ID)).andExpect(status().isNoContent());
        verify(userRoleService).assignRole(USER_ID, 7L);
    }

    @Test
    void assignRole_unknownUserOrRoleIs404() throws Exception {
        org.mockito.Mockito.doThrow(new EntityNotFoundException("User not found: " + USER_ID))
                .when(userRoleService).assignRole(USER_ID, 7L);

        mvc.perform(put("/roles/7/users/" + USER_ID)).andExpect(status().isNotFound());
    }

    @Test
    void assignRole_userServiceDownIs503() throws Exception {
        org.mockito.Mockito.doThrow(new ServiceUnavailableException("user-service is unavailable", null))
                .when(userRoleService).assignRole(USER_ID, 7L);

        mvc.perform(put("/roles/7/users/" + USER_ID)).andExpect(status().isServiceUnavailable());
    }

    @Test
    void assignRole_badUuidIs400() throws Exception {
        mvc.perform(put("/roles/7/users/not-a-uuid")).andExpect(status().isBadRequest());
        verify(userRoleService, never()).assignRole(any(), any());
    }

    @Test
    void revokeRole_returns204() throws Exception {
        mvc.perform(delete("/roles/7/users/" + USER_ID)).andExpect(status().isNoContent());
        verify(userRoleService).revokeRole(USER_ID, 7L);
    }

    @Test
    void unknownPathIs404NotAnInternalError() throws Exception {
        mvc.perform(post("/roles/7/nothing")).andExpect(status().isNotFound());
    }
}
