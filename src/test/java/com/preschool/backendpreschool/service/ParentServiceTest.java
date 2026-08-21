package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ParentRequest;
import com.preschool.backendpreschool.dto.ParentResponse;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.ParentStatus;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ParentService parentService;

    @Test
    void createParentWithPasswordCreatesLinkedParentUser() {
        Role parentRole = Role.builder()
                .roleId(6L)
                .code(RoleName.PARENT)
                .name("Parent")
                .build();

        ParentRequest request = new ParentRequest(
                " Maria ",
                " Andersson ",
                "MARIA@example.com",
                " 0701234567 ",
                "Demo Street",
                "es",
                null,
                "Primary contact",
                "123456"
        );

        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0701234567")).thenReturn(false);
        when(roleRepository.findByCode(RoleName.PARENT)).thenReturn(Optional.of(parentRole));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> {
            Parent parent = invocation.getArgument(0);
            parent.setParentId(20L);
            return parent;
        });

        ParentResponse response = parentService.createParent(request);

        assertThat(response.parentId()).isEqualTo(20L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("maria@example.com");
        assertThat(response.phone()).isEqualTo("0701234567");
        assertThat(response.status()).isEqualTo(ParentStatus.ACTIVE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("maria@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getRoles()).extracting(Role::getCode).containsExactly(RoleName.PARENT);
    }

    @Test
    void deleteParentSoftDeletesInsteadOfRemovingRow() {
        Parent parent = buildParent();

        when(parentRepository.findByParentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(parent));
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        parentService.deleteParent(1L);

        assertThat(parent.getDeletedAt()).isNotNull();
        verify(parentRepository, never()).delete(any(Parent.class));
        verify(parentRepository).save(parent);
    }

    @Test
    void restoreParentWithinGraceWindowClearsDeletedAt() {
        Parent parent = buildParent();
        parent.setDeletedAt(LocalDateTime.now().minusDays(3));

        when(parentRepository.findByParentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(parent));
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParentResponse response = parentService.restoreParent(1L);

        assertThat(parent.getDeletedAt()).isNull();
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void restoreParentAfterGraceWindowThrowsConflict() {
        Parent parent = buildParent();
        parent.setDeletedAt(LocalDateTime.now().minusDays(8));

        when(parentRepository.findByParentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> parentService.restoreParent(1L))
                .isInstanceOf(ConflictException.class);

        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void restoreParentNotDeletedThrowsNotFound() {
        when(parentRepository.findByParentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parentService.restoreParent(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllParentsWithIncludeDeletedTrueReturnsSoftDeletedParents() {
        Parent deleted = buildParent();
        deleted.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(parentRepository.findAll()).thenReturn(List.of(deleted));

        List<ParentResponse> response = parentService.getAllParents(null, null, true);

        assertThat(response).extracting(ParentResponse::parentId).containsExactly(1L);
    }

    @Test
    void purgeExpiredSoftDeletedParentsDeletesExpiredAndSkipsRestrictedOnes() {
        Parent purgeable = buildParent();
        Parent restricted = Parent.builder()
                .parentId(2L)
                .firstName("Sofia")
                .lastName("Guardian")
                .status(ParentStatus.ACTIVE)
                .build();

        when(parentRepository.findAllByDeletedAtIsNotNullAndDeletedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(purgeable, restricted));
        doNothing().when(parentRepository).delete(purgeable);
        doThrow(new DataIntegrityViolationException("has related consents"))
                .when(parentRepository).delete(restricted);

        parentService.purgeExpiredSoftDeletedParents();

        verify(parentRepository).delete(purgeable);
        verify(parentRepository).delete(restricted);
    }

    private Parent buildParent() {
        return Parent.builder()
                .parentId(1L)
                .firstName("Maria")
                .lastName("Andersson")
                .email("maria@example.com")
                .phone("0701234567")
                .status(ParentStatus.ACTIVE)
                .build();
    }
}
