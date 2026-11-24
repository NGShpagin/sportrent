package ru.saschoolsport.user.dto;

import lombok.*;
import ru.saschoolsport.user.entity.User;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactInfoDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;

    public static UserContactInfoDto fromEntity(User user) {
        return UserContactInfoDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
