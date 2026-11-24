package ru.saschoolsport.authz.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.saschoolsport.authz.client.UserServiceClient;
import ru.saschoolsport.authz.dto.UserDto;
import ru.saschoolsport.authz.entity.Role;
import ru.saschoolsport.authz.entity.UserRole;
import ru.saschoolsport.authz.exception.ServiceUnavailableException;
import ru.saschoolsport.authz.repository.RoleRepository;
import ru.saschoolsport.authz.repository.UserRoleRepository;
import ru.saschoolsport.authz.support.FeignTestSupport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Long ROLE_ID = 7L;

    @Mock UserServiceClient userServiceClient;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @InjectMocks UserRoleService service;

    private Role role() {
        Role role = new Role("ROLE_X", "X", null);
        role.setId(ROLE_ID);
        return role;
    }

    @Test
    void assignRole_savesWhenRoleAndUserExist() {
        Role role = role();
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.of(role));
        when(userServiceClient.getUser(USER, null)).thenReturn(new UserDto());
        when(userRoleRepository.existsByUserIdAndRoleId(USER, ROLE_ID)).thenReturn(false);

        service.assignRole(USER, ROLE_ID);

        ArgumentCaptor<UserRole> saved = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER);
        assertThat(saved.getValue().getRole()).isSameAs(role);
    }

    @Test
    void assignRole_unknownRoleFailsBeforeCallingUserService() {
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRole(USER, ROLE_ID)).isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(userServiceClient);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRole_unknownUserIsNotFoundAndNothingIsSaved() {
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.of(role()));
        when(userServiceClient.getUser(USER, null)).thenThrow(FeignTestSupport.notFound());

        assertThatThrownBy(() -> service.assignRole(USER, ROLE_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(USER.toString());

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRole_userServiceDownPropagatesAndNothingIsSaved() {
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.of(role()));
        when(userServiceClient.getUser(USER, null)).thenThrow(new ServiceUnavailableException("down", null));

        assertThatThrownBy(() -> service.assignRole(USER, ROLE_ID)).isInstanceOf(ServiceUnavailableException.class);

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRole_isIdempotentWhenAlreadyAssigned() {
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.of(role()));
        when(userServiceClient.getUser(USER, null)).thenReturn(new UserDto());
        when(userRoleRepository.existsByUserIdAndRoleId(USER, ROLE_ID)).thenReturn(true);

        service.assignRole(USER, ROLE_ID);

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRole_concurrentInsertIsSwallowed() {
        when(roleRepository.findByIdAndDeletedAtIsNull(ROLE_ID)).thenReturn(Optional.of(role()));
        when(userServiceClient.getUser(USER, null)).thenReturn(new UserDto());
        when(userRoleRepository.existsByUserIdAndRoleId(USER, ROLE_ID)).thenReturn(false);
        when(userRoleRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        service.assignRole(USER, ROLE_ID); // must not throw
    }

    @Test
    void revokeRole_deletesAssignment() {
        service.revokeRole(USER, ROLE_ID);
        verify(userRoleRepository).deleteByUserIdAndRoleId(USER, ROLE_ID);
    }

    @Test
    void getUserIdsByRoles_resolvesByCodeDeduplicatesAndSkipsUnknown() {
        UUID shared = UUID.randomUUID();
        UUID onlyInB = UUID.randomUUID();
        when(roleRepository.findActiveIdByCode("ROLE_A")).thenReturn(Optional.of(1L));
        when(roleRepository.findActiveIdByCode("ROLE_B")).thenReturn(Optional.of(2L));
        when(roleRepository.findActiveIdByCode("ROLE_GONE")).thenReturn(Optional.empty());
        when(userRoleRepository.findUserIdsByRoleId(1L)).thenReturn(List.of(shared));
        when(userRoleRepository.findUserIdsByRoleId(2L)).thenReturn(List.of(shared, onlyInB));

        List<UUID> result = service.getUserIdsByRoles(List.of("ROLE_A", "ROLE_GONE", "ROLE_B"));

        assertThat(result).containsExactly(shared, onlyInB);
    }
}
