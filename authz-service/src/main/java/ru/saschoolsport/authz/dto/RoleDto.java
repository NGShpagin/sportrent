package ru.saschoolsport.authz.dto;

import ru.saschoolsport.authz.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<UserPermissionDto> permissions;

    public static RoleDto fromEntity(Role role) {
        List<UserPermissionDto> permissionDtos = role.getPermissions() == null
                ? List.of()
                : role.getPermissions().stream()
                .map(UserPermissionDto::fromEntity)
                .toList();

        return RoleDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionDtos)
                .build();
    }

    public static List<RoleDto> fromEntities(List<Role> roles) {
        return roles.stream()
                .map(RoleDto::fromEntity)
                .toList();
    }
}
