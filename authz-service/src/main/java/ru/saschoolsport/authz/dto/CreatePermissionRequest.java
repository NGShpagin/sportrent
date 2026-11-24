package ru.saschoolsport.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    private static final String IDENTIFIER = "^[a-z][a-z0-9_-]*$";

    @NotBlank(message = "Resource is required")
    @Size(max = 50, message = "Resource must be at most 50 characters")
    @Pattern(regexp = IDENTIFIER, message = "Resource must be lowercase letters, digits, '_' or '-', starting with a letter")
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(max = 20, message = "Action must be at most 20 characters")
    @Pattern(regexp = IDENTIFIER, message = "Action must be lowercase letters, digits, '_' or '-', starting with a letter")
    private String action;

    @Size(max = 200, message = "Description must be at most 200 characters")
    private String description;
}
