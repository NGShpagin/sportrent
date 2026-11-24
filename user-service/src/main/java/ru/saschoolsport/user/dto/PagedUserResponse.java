package ru.saschoolsport.user.dto;

import ru.saschoolsport.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedUserResponse {

    private List<UserDto> data;
    private Pagination pagination;

    public static PagedUserResponse from(Page<User> page) {
        return PagedUserResponse.builder()
                .data(page.getContent().stream().map(UserDto::fromEntity).toList())
                .pagination(Pagination.from(page))
                .build();
    }
}
