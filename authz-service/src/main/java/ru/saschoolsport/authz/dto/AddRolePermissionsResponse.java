package ru.saschoolsport.authz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRolePermissionsResponse {
    /** Requested permission ids that do not exist. */
    List<Long> errorPermissions;
    /** Requested permission ids that exist and are now granted to the role. */
    List<Long> successPermissions;
}
