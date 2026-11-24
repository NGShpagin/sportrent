package ru.saschoolsport.authz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleResponseDto {
    private List<Long> notFoundPermissionIds;
}
