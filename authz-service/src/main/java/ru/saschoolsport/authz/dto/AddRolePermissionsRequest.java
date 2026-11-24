package ru.saschoolsport.authz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRolePermissionsRequest {
    @NotEmpty(message = "Permissions list must not be empty")
    List<@NotNull(message = "Permission id must not be null") Long> permissions;
}
