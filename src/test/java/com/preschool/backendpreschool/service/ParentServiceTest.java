package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ParentRequest;
import com.preschool.backendpreschool.dto.ParentResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
