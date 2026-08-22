package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.AssignRoleRequest;
import com.preschool.backendpreschool.dto.CreateUserRequest;
import com.preschool.backendpreschool.dto.UserResponse;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void adminCanAssignSameRankRoleToAnotherUser() {
        User admin = buildUser(1L, "admin@school.com", role(RoleName.ADMIN, 90));
        User target = buildUser(2L, "target@school.com", role(RoleName.TEACHER, 10));
        Role directorRole = role(RoleName.DIRECTOR, 90);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(roleRepository.findByCode(RoleName.DIRECTOR)).thenReturn(Optional.of(directorRole));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.assignRole(2L, new AssignRoleRequest(RoleName.DIRECTOR), "admin@school.com");

        assertThat(response.roles()).extracting(r -> r.code()).contains(RoleName.TEACHER, RoleName.DIRECTOR);
    }

    @Test
    void adminCannotAssignSuperAdminRole() {
        User admin = buildUser(1L, "admin@school.com", role(RoleName.ADMIN, 90));
        Role superAdminRole = role(RoleName.SUPER_ADMIN, 100);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(roleRepository.findByCode(RoleName.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));

        assertThatThrownBy(() -> userService.assignRole(2L, new AssignRoleRequest(RoleName.SUPER_ADMIN), "admin@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void teacherCannotAssignAdminRole() {
        User teacher = buildUser(1L, "teacher@school.com", role(RoleName.TEACHER, 10));
        Role adminRole = role(RoleName.ADMIN, 90);

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(roleRepository.findByCode(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));

        assertThatThrownBy(() -> userService.assignRole(2L, new AssignRoleRequest(RoleName.ADMIN), "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cannotRemoveSuperAdminFromLastRemainingSuperAdmin() {
        User superAdmin = buildUser(1L, "super@school.com", role(RoleName.SUPER_ADMIN, 100));
        Role superAdminRole = role(RoleName.SUPER_ADMIN, 100);

        when(userRepository.findByEmail("super@school.com")).thenReturn(Optional.of(superAdmin));
        when(roleRepository.findByCode(RoleName.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.countByRolesCode(RoleName.SUPER_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.removeRole(1L, new AssignRoleRequest(RoleName.SUPER_ADMIN), "super@school.com"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createUserRejectsRoleAboveRequesterRank() {
        User admin = buildUser(1L, "admin@school.com", role(RoleName.ADMIN, 90));
        Role superAdminRole = role(RoleName.SUPER_ADMIN, 100);

        when(roleRepository.findByCode(RoleName.SUPER_ADMIN)).thenReturn(Optional.of(superAdminRole));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        CreateUserRequest request = new CreateUserRequest(
                "new@school.com", null, "123456", Set.of(RoleName.SUPER_ADMIN)
        );

        assertThatThrownBy(() -> userService.createUser(request, "admin@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createUserAllowsRoleAtRequesterOwnRank() {
        User director = buildUser(1L, "director@school.com", role(RoleName.DIRECTOR, 90));
        Role teacherRole = role(RoleName.TEACHER, 10);

        when(roleRepository.findByCode(RoleName.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.existsByEmail("new@school.com")).thenReturn(false);
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(5L);
            return user;
        });

        CreateUserRequest request = new CreateUserRequest(
                "new@school.com", null, "123456", Set.of(RoleName.TEACHER)
        );

        UserResponse response = userService.createUser(request, "director@school.com");

        assertThat(response.userId()).isEqualTo(5L);
    }

    private User buildUser(Long userId, String email, Role... roles) {
        return User.builder()
                .userId(userId)
                .email(email)
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(Set.of(roles)))
                .build();
    }

    private Role role(RoleName code, int rankLevel) {
        return Role.builder()
                .roleId((long) rankLevel)
                .code(code)
                .name(code.name())
                .rankLevel(rankLevel)
                .build();
    }
}
