package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentGuardianSummary;
import com.preschool.backendpreschool.dto.StudentProfilePhotoRequest;
import com.preschool.backendpreschool.dto.StudentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsentType;
import com.preschool.backendpreschool.model.StudentGuardian;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
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
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private StudentConsentRepository studentConsentRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void getStudentsIncludesAllGuardiansAndDerivesPrimaryFromBatchLookup() {
        Student student = buildStudent();
        StudentGuardian primaryGuardian = StudentGuardian.builder()
                .student(student)
                .parent(Parent.builder().parentId(1L).firstName("Luis").lastName("Diaz").email("luis@example.com").build())
                .relationshipType(com.preschool.backendpreschool.model.GuardianRelationshipType.FATHER)
                .primaryContact(true)
                .build();
        StudentGuardian secondaryGuardian = StudentGuardian.builder()
                .student(student)
                .parent(Parent.builder().parentId(2L).firstName("Carla").lastName("Diaz").build())
                .relationshipType(com.preschool.backendpreschool.model.GuardianRelationshipType.MOTHER)
                .primaryContact(false)
                .build();

        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentGuardianRepository.findByStudentStudentIdIn(List.of(1L)))
                .thenReturn(List.of(primaryGuardian, secondaryGuardian));

        List<StudentResponse> response = studentService.getStudents(null, null, null, null);

        assertThat(response).singleElement().satisfies(dto -> {
            assertThat(dto.primaryGuardianName()).isEqualTo("Luis Diaz");
            assertThat(dto.guardians()).hasSize(2);
            assertThat(dto.guardians())
                    .extracting(StudentGuardianSummary::parentName)
                    .containsExactlyInAnyOrder("Luis Diaz", "Carla Diaz");
            assertThat(dto.guardians())
                    .filteredOn(StudentGuardianSummary::parentId, 1L)
                    .singleElement()
                    .extracting(StudentGuardianSummary::email)
                    .isEqualTo("luis@example.com");
        });
    }

    @Test
    void getStudentsFiltersBySearchAcrossNameAndCode() {
        Student ana = buildStudent();
        Student noah = Student.builder()
                .studentId(2L)
                .studentCode("STU-002")
                .firstName("Noah")
                .lastName("Eriksson")
                .birthDate(LocalDate.of(2021, 1, 20))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 2, 1))
                .build();

        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(ana, noah));
        when(studentGuardianRepository.findByStudentStudentIdIn(List.of(1L)))
                .thenReturn(List.of());

        List<StudentResponse> byName = studentService.getStudents("ana", null, null, null);
        List<StudentResponse> byCode = studentService.getStudents("stu-001", null, null, null);

        assertThat(byName).extracting(StudentResponse::studentId).containsExactly(1L);
        assertThat(byCode).extracting(StudentResponse::studentId).containsExactly(1L);
    }

    @Test
    void getStudentsFiltersByGroupAndStatus() {
        Student activeInGroup = buildStudent();
        ClassGroup group = ClassGroup.builder().groupId(2L).name("Rainbow Room").build();
        activeInGroup.setClassGroup(group);

        Student inactiveNoGroup = Student.builder()
                .studentId(3L)
                .firstName("Oliver")
                .lastName("Brown")
                .birthDate(LocalDate.of(2021, 5, 29))
                .status(StudentStatus.inactive)
                .enrollmentDate(LocalDate.of(2024, 3, 1))
                .build();

        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(activeInGroup, inactiveNoGroup));
        when(studentGuardianRepository.findByStudentStudentIdIn(List.of(1L)))
                .thenReturn(List.of());

        List<StudentResponse> byGroup = studentService.getStudents(null, 2L, null, null);
        List<StudentResponse> byStatus = studentService.getStudents(null, null, StudentStatus.inactive, null);

        assertThat(byGroup).extracting(StudentResponse::studentId).containsExactly(1L);
        assertThat(byStatus).extracting(StudentResponse::studentId).containsExactly(3L);
    }

    @Test
    void getStudentByIdIncludesAllGuardians() {
        Student student = buildStudent();
        StudentGuardian guardian = StudentGuardian.builder()
                .student(student)
                .parent(Parent.builder().parentId(1L).firstName("Luis").lastName("Diaz").build())
                .relationshipType(com.preschool.backendpreschool.model.GuardianRelationshipType.FATHER)
                .primaryContact(true)
                .build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(studentGuardianRepository.findByStudentStudentId(1L)).thenReturn(List.of(guardian));

        StudentResponse response = studentService.getStudentById(1L);

        assertThat(response.guardians()).singleElement()
                .extracting(StudentGuardianSummary::parentName)
                .isEqualTo("Luis Diaz");
        assertThat(response.primaryGuardianName()).isEqualTo("Luis Diaz");
    }

    @Test
    void updateProfilePhotoStoresTrimmedUrl() {
        Student student = buildStudent();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                1L,
                StudentConsentType.IMAGE_PROFILE_PHOTO
        )).thenReturn(true);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.updateProfilePhoto(
                1L,
                new StudentProfilePhotoRequest("  https://cdn.example.com/students/1/profile.jpg  ")
        );

        assertThat(response.profilePhotoUrl()).isEqualTo("https://cdn.example.com/students/1/profile.jpg");
        assertThat(student.getProfilePhotoUrl()).isEqualTo("https://cdn.example.com/students/1/profile.jpg");
    }

    @Test
    void updateProfilePhotoRequiresActiveImageConsent() {
        Student student = buildStudent();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                1L,
                StudentConsentType.IMAGE_PROFILE_PHOTO
        )).thenReturn(false);

        assertThatThrownBy(() -> studentService.updateProfilePhoto(
                1L,
                new StudentProfilePhotoRequest("https://cdn.example.com/students/1/profile.jpg")
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Se requiere consentimiento activo de imagen para asignar foto de perfil");
    }

    @Test
    void removeProfilePhotoClearsUrl() {
        Student student = buildStudent();
        student.setProfilePhotoUrl("https://cdn.example.com/students/1/profile.jpg");

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.removeProfilePhoto(1L);

        assertThat(response.profilePhotoUrl()).isNull();
        assertThat(student.getProfilePhotoUrl()).isNull();
    }

    @Test
    void deleteStudentSoftDeletesInsteadOfRemovingRow() {
        Student student = buildStudent();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.deleteStudent(1L);

        assertThat(student.getDeletedAt()).isNotNull();
        verify(studentRepository, never()).delete(any(Student.class));
        verify(studentRepository).save(student);
    }

    @Test
    void deleteStudentAlreadyDeletedThrowsNotFound() {
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deleteStudent(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void restoreStudentWithinGraceWindowClearsDeletedAt() {
        Student student = buildStudent();
        student.setDeletedAt(LocalDateTime.now().minusDays(3));

        when(studentRepository.findByStudentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentGuardianRepository.findByStudentStudentId(1L)).thenReturn(List.of());

        StudentResponse response = studentService.restoreStudent(1L);

        assertThat(student.getDeletedAt()).isNull();
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void restoreStudentAfterGraceWindowThrowsConflict() {
        Student student = buildStudent();
        student.setDeletedAt(LocalDateTime.now().minusDays(8));

        when(studentRepository.findByStudentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.restoreStudent(1L))
                .isInstanceOf(ConflictException.class);

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void restoreStudentNotDeletedThrowsNotFound() {
        when(studentRepository.findByStudentIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.restoreStudent(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStudentsWithIncludeDeletedTrueReturnsSoftDeletedStudents() {
        Student deleted = buildStudent();
        deleted.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(studentRepository.findAll()).thenReturn(List.of(deleted));
        when(studentGuardianRepository.findByStudentStudentIdIn(List.of(1L))).thenReturn(List.of());

        List<StudentResponse> response = studentService.getStudents(null, null, null, true);

        assertThat(response).extracting(StudentResponse::studentId).containsExactly(1L);
        verify(studentRepository, never()).findAllByDeletedAtIsNull();
    }

    @Test
    void purgeExpiredSoftDeletedStudentsDeletesExpiredAndSkipsRestrictedOnes() {
        Student purgeable = buildStudent();
        Student restricted = Student.builder()
                .studentId(2L)
                .firstName("Noah")
                .lastName("Eriksson")
                .birthDate(LocalDate.of(2021, 1, 20))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 2, 1))
                .build();

        when(studentRepository.findAllByDeletedAtIsNotNullAndDeletedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(purgeable, restricted));
        doNothing().when(studentRepository).delete(purgeable);
        doThrow(new DataIntegrityViolationException("has related charges"))
                .when(studentRepository).delete(restricted);

        studentService.purgeExpiredSoftDeletedStudents();

        verify(studentRepository).delete(purgeable);
        verify(studentRepository).delete(restricted);
    }

    private Student buildStudent() {
        return Student.builder()
                .studentId(1L)
                .studentCode("STU-001")
                .firstName("Ana")
                .lastName("Diaz")
                .birthDate(LocalDate.of(2020, 5, 10))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 8, 15))
                .build();
    }
}
