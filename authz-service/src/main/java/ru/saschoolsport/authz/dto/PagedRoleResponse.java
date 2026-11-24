package ru.saschoolsport.authz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import ru.saschoolsport.authz.entity.Role;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedRoleResponse {
    private List<PagedRole> data;
    private Pagination pagination;

    public static PagedRoleResponse from(Page<Role> page) {
        return PagedRoleResponse.builder()
                .data(page.getContent().stream().map(PagedRole::fromEntity).toList())
                .pagination(Pagination.from(page))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedRole {
        private Long id;
        private String code;
        private String name;
        private String description;

        public static PagedRole fromEntity(Role entity) {
            return PagedRole.builder()
                    .id(entity.getId())
                    .code(entity.getCode())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .build();
        }
    }
}
