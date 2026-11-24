package ru.saschoolsport.user.dto;

import ru.saschoolsport.user.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private LocalDate birthDate;
    public static UserProfileDto fromEntity(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .build();
    }
}
