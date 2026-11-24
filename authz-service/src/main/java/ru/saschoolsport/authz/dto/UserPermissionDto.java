package ru.saschoolsport.authz.dto;

import ru.saschoolsport.authz.entity.UserPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionDto {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private Instant createdAt;

    public static UserPermissionDto fromEntity(UserPermission permission) {
        return UserPermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .createdAt(permission.getCreatedAt())
                .build();
    }

    public static List<UserPermissionDto> fromEntities(List<UserPermission> userPermissions) {
        return userPermissions.stream()
                .map(UserPermissionDto::fromEntity)
                .toList();
    }
}
