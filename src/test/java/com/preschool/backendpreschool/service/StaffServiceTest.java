package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StaffRequest;
import com.preschool.backendpreschool.dto.StaffResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StaffService staffService;

    @Test
    void createStaffWithoutPasswordCreatesRecordWithoutLoginAccount() {
        StaffRequest request = new StaffRequest(
                "Sara", "Assistant", null, "+46000000099", "STAFF-010",
                "Assistant Teacher", "teacher", LocalDate.of(2026, 8, 1), "Demo notes", null, null
        );

        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> {
            Staff staff = invocation.getArgument(0);
            staff.setStaffId(10L);
            return staff;
        });

        StaffResponse response = staffService.createStaff(request, "admin@school.com");

        assertThat(response.staffId()).isEqualTo(10L);
        assertThat(response.userId()).isNull();
        assertThat(response.firstName()).isEqualTo("Sara");
        assertThat(response.roles()).isEmpty();
    }

    @Test
    void createStaffWithPasswordCreatesLinkedUserWithAllowedRoles() {
        User admin = buildUser(RoleName.ADMIN, 90);
        Role teacherRole = role(RoleName.TEACHER, 10);

        StaffRequest request = new StaffRequest(
                "Diana", "New", "diana.new@school.com", null, "STAFF-011",
                "Lead Teacher", "teacher", LocalDate.of(2026, 8, 1), null, "123456", Set.of(RoleName.TEACHER)
        );

        when(userRepository.existsByEmail("diana.new@school.com")).thenReturn(false);
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(roleRepository.findByCode(RoleName.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(20L);
            return user;
        });
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> {
            Staff staff = invocation.getArgument(0);
            staff.setStaffId(11L);
            return staff;
        });

        StaffResponse response = staffService.createStaff(request, "admin@school.com");

        assertThat(response.userId()).isEqualTo(20L);
        assertThat(response.roles()).extracting(r -> r.code()).containsExactly(RoleName.TEACHER);
    }

    @Test
    void createStaffRejectsRoleAboveRequesterRank() {
        User teacher = buildUser(RoleName.TEACHER, 10);
        Role adminRole = role(RoleName.ADMIN, 90);

        StaffRequest request = new StaffRequest(
                "New", "Admin", "new.admin@school.com", null, null,
                "Office Admin", "admin", null, null, "123456", Set.of(RoleName.ADMIN)
        );

        when(userRepository.existsByEmail("new.admin@school.com")).thenReturn(false);
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(roleRepository.findByCode(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));

        assertThatThrownBy(() -> staffService.createStaff(request, "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createStaffWithPasswordButNoRolesIsRejected() {
        StaffRequest request = new StaffRequest(
                "New", "Person", "new.person@school.com", null, null,
                "Office Admin", "admin", null, null, "123456", null
        );

        when(userRepository.existsByEmail("new.person@school.com")).thenReturn(false);

        assertThatThrownBy(() -> staffService.createStaff(request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    private User buildUser(RoleName roleName, int rank) {
        return User.builder()
                .userId(1L)
                .email(roleName.name().toLowerCase() + "@school.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role(roleName, rank)))
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
