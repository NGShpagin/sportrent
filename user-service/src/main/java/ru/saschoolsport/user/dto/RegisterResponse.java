package ru.saschoolsport.user.dto;

import ru.saschoolsport.user.entity.User;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private UUID id;
    private String username;

    // Конвертер из Entity в DTO (без пароля!)
    public static RegisterResponse fromEntity(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}
