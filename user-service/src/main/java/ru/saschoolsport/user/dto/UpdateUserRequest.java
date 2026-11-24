package ru.saschoolsport.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s-]+$", message = "First name can only contain letters, spaces and hyphens")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s-]*$", message = "Last name can only contain letters, spaces and hyphens")
    private String lastName;

    @Size(max = 50, message = "Middle name must be less than 50 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s-]*$", message = "Middle name can only contain letters, spaces and hyphens")
    private String middleName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}
