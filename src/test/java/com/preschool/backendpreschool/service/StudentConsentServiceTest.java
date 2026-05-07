package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentConsentRequest;
import com.preschool.backendpreschool.dto.StudentConsentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsent;
import com.preschool.backendpreschool.model.StudentConsentType;
import com.preschool.backendpreschool.model.StudentGuardianId;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentConsentServiceTest {

    @Mock
    private StudentConsentRepository studentConsentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    @InjectMocks
    private StudentConsentService studentConsentService;

    @Test
    void linkedParentCanAcceptOwnStudentConsent() {
        Student student = buildStudent(1L);
        User parentUser = buildUser(2L, "parent@school.com", RoleName.PARENT);
        Parent parent = buildParent(3L, parentUser);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("parent@school.com")).thenReturn(Optional.of(parentUser));
        when(parentRepository.findByUserEmail("parent@school.com")).thenReturn(Optional.of(parent));
        when(studentGuardianRepository.existsById(new StudentGuardianId(1L, 3L))).thenReturn(true);
        when(studentConsentRepository.findByStudentStudentIdAndParentParentIdAndConsentTypeAndRevokedAtIsNull(
                1L,
                3L,
                StudentConsentType.IMAGE_PROFILE_PHOTO
        )).thenReturn(Optional.empty());
        when(studentConsentRepository.save(any(StudentConsent.class))).thenAnswer(invocation -> {
            StudentConsent consent = invocation.getArgument(0);
            consent.setStudentConsentId(9L);
            return consent;
        });

        StudentConsentResponse response = studentConsentService.acceptConsent(
                1L,
                new StudentConsentRequest(null, StudentConsentType.IMAGE_PROFILE_PHOTO, "  OK image  "),
                "parent@school.com"
        );

        assertThat(response.studentConsentId()).isEqualTo(9L);
        assertThat(response.parentId()).isEqualTo(3L);
        assertThat(response.consentType()).isEqualTo(StudentConsentType.IMAGE_PROFILE_PHOTO);
        assertThat(response.active()).isTrue();
        assertThat(response.notes()).isEqualTo("OK image");
    }

    @Test
    void parentCannotAcceptConsentForUnlinkedStudent() {
        Student student = buildStudent(1L);
        User parentUser = buildUser(2L, "parent@school.com", RoleName.PARENT);
        Parent parent = buildParent(3L, parentUser);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("parent@school.com")).thenReturn(Optional.of(parentUser));
        when(parentRepository.findByUserEmail("parent@school.com")).thenReturn(Optional.of(parent));
        when(studentGuardianRepository.existsById(new StudentGuardianId(1L, 3L))).thenReturn(false);

        assertThatThrownBy(() -> studentConsentService.acceptConsent(
                1L,
                new StudentConsentRequest(null, StudentConsentType.PHOTO_ALBUM, null),
                "parent@school.com"
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("El tutor no esta vinculado a este estudiante");
    }

    @Test
    void adminMustProvideParentIdWhenAcceptingConsent() {
        Student student = buildStudent(1L);
        User admin = buildUser(4L, "admin@school.com", RoleName.ADMIN);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> studentConsentService.acceptConsent(
                1L,
                new StudentConsentRequest(null, StudentConsentType.PHOTO_ALBUM, null),
                "admin@school.com"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("parentId es requerido para registrar consentimiento desde administracion");
    }

    @Test
    void revokeConsentMarksItInactive() {
        Student student = buildStudent(1L);
        User admin = buildUser(4L, "admin@school.com", RoleName.ADMIN);
        Parent parent = buildParent(3L, buildUser(2L, "parent@school.com", RoleName.PARENT));
        StudentConsent consent = StudentConsent.builder()
                .studentConsentId(9L)
                .student(student)
                .parent(parent)
                .recordedByUser(admin)
                .consentType(StudentConsentType.PHOTO_ALBUM)
                .granted(true)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentConsentRepository.findById(9L)).thenReturn(Optional.of(consent));
        when(studentGuardianRepository.existsById(new StudentGuardianId(1L, 3L))).thenReturn(true);
        when(studentConsentRepository.save(any(StudentConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentConsentResponse response = studentConsentService.revokeConsent(1L, 9L, "admin@school.com");

        assertThat(response.active()).isFalse();
        assertThat(consent.getGranted()).isFalse();
        assertThat(consent.getRevokedAt()).isNotNull();
    }

    private Student buildStudent(Long studentId) {
        return Student.builder()
                .studentId(studentId)
                .firstName("Ana")
                .lastName("Diaz")
                .birthDate(LocalDate.of(2020, 5, 10))
                .classGroup(ClassGroup.builder().groupId(10L).name("Group A").build())
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 8, 15))
                .build();
    }

    private Parent buildParent(Long parentId, User user) {
        return Parent.builder()
                .parentId(parentId)
                .user(user)
                .firstName("Maria")
                .lastName("Diaz")
                .email(user.getEmail())
                .build();
    }

    private User buildUser(Long userId, String email, RoleName roleName) {
        return User.builder()
                .userId(userId)
                .email(email)
                .roles(Set.of(Role.builder().roleId(userId).code(roleName).name(roleName.name()).build()))
                .build();
    }
}
