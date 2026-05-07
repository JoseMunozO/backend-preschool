package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.PhotoAlbumPhotoRequest;
import com.preschool.backendpreschool.dto.PhotoAlbumPhotoResponse;
import com.preschool.backendpreschool.dto.PhotoAlbumRequest;
import com.preschool.backendpreschool.dto.PhotoAlbumResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.PhotoAlbum;
import com.preschool.backendpreschool.model.PhotoAlbumPhoto;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.StaffGroupRole;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsentType;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.PhotoAlbumPhotoRepository;
import com.preschool.backendpreschool.repository.PhotoAlbumRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoAlbumServiceTest {

    @Mock
    private PhotoAlbumRepository photoAlbumRepository;

    @Mock
    private PhotoAlbumPhotoRepository photoAlbumPhotoRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentConsentRepository studentConsentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    @InjectMocks
    private PhotoAlbumService photoAlbumService;

    @Test
    void adminCanCreateStudentAlbumWhenPhotoConsentExists() {
        User admin = buildUser(1L, "admin@school.com", RoleName.ADMIN);
        Student student = buildStudent(10L, 5L);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                10L,
                StudentConsentType.PHOTO_ALBUM
        )).thenReturn(true);
        when(photoAlbumRepository.save(any(PhotoAlbum.class))).thenAnswer(invocation -> {
            PhotoAlbum album = invocation.getArgument(0);
            album.setPhotoAlbumId(20L);
            return album;
        });
        when(photoAlbumPhotoRepository.findByPhotoAlbumPhotoAlbumIdAndDeletedFalseOrderByCreatedAtDesc(20L))
                .thenReturn(List.of());

        PhotoAlbumResponse response = photoAlbumService.createAlbum(
                new PhotoAlbumRequest("  Spring trip  ", "  Photos from trip  ", null, 10L, LocalDate.of(2026, 5, 7)),
                "admin@school.com"
        );

        assertThat(response.photoAlbumId()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("Spring trip");
        assertThat(response.description()).isEqualTo("Photos from trip");
        assertThat(response.studentId()).isEqualTo(10L);
    }

    @Test
    void createStudentAlbumRequiresPhotoConsent() {
        User admin = buildUser(1L, "admin@school.com", RoleName.ADMIN);
        Student student = buildStudent(10L, 5L);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                10L,
                StudentConsentType.PHOTO_ALBUM
        )).thenReturn(false);

        assertThatThrownBy(() -> photoAlbumService.createAlbum(
                new PhotoAlbumRequest("Album", null, null, 10L, null),
                "admin@school.com"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Se requiere consentimiento activo PHOTO_ALBUM para usar fotos de este estudiante");
    }

    @Test
    void assignedTeacherCanAddUnapprovedPhotoToOwnGroupAlbum() {
        User teacher = buildUser(2L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).user(teacher).build();
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        PhotoAlbum album = PhotoAlbum.builder()
                .photoAlbumId(20L)
                .title("Group album")
                .classGroup(group)
                .createdByUser(teacher)
                .active(true)
                .build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(photoAlbumRepository.findById(20L)).thenReturn(Optional.of(album));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(staff, group)));
        when(photoAlbumPhotoRepository.save(any(PhotoAlbumPhoto.class))).thenAnswer(invocation -> {
            PhotoAlbumPhoto photo = invocation.getArgument(0);
            photo.setPhotoAlbumPhotoId(30L);
            return photo;
        });

        PhotoAlbumPhotoResponse response = photoAlbumService.addPhoto(
                20L,
                new PhotoAlbumPhotoRequest(null, "  https://cdn.example.com/photo.jpg  ", "  Outdoor play  "),
                "teacher@school.com"
        );

        assertThat(response.photoAlbumPhotoId()).isEqualTo(30L);
        assertThat(response.photoUrl()).isEqualTo("https://cdn.example.com/photo.jpg");
        assertThat(response.caption()).isEqualTo("Outdoor play");
        assertThat(response.approved()).isFalse();
    }

    @Test
    void teacherCannotAccessAlbumOutsideAssignedGroup() {
        User teacher = buildUser(2L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).user(teacher).build();
        PhotoAlbum album = PhotoAlbum.builder()
                .photoAlbumId(20L)
                .title("Group album")
                .classGroup(ClassGroup.builder().groupId(5L).name("Group A").build())
                .createdByUser(teacher)
                .active(true)
                .build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(photoAlbumRepository.findById(20L)).thenReturn(Optional.of(album));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> photoAlbumService.getAlbum(20L, "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No tienes permiso para gestionar este album");
    }

    @Test
    void adminCanApprovePhoto() {
        User admin = buildUser(1L, "admin@school.com", RoleName.ADMIN);
        PhotoAlbum album = PhotoAlbum.builder()
                .photoAlbumId(20L)
                .title("Album")
                .createdByUser(admin)
                .active(true)
                .build();
        PhotoAlbumPhoto photo = PhotoAlbumPhoto.builder()
                .photoAlbumPhotoId(30L)
                .photoAlbum(album)
                .uploadedByUser(admin)
                .photoUrl("https://cdn.example.com/photo.jpg")
                .approved(false)
                .deleted(false)
                .build();

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(photoAlbumRepository.findById(20L)).thenReturn(Optional.of(album));
        when(photoAlbumPhotoRepository.findById(30L)).thenReturn(Optional.of(photo));
        when(photoAlbumPhotoRepository.save(any(PhotoAlbumPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PhotoAlbumPhotoResponse response = photoAlbumService.approvePhoto(20L, 30L, "admin@school.com");

        assertThat(response.approved()).isTrue();
        assertThat(photo.getApproved()).isTrue();
    }

    private Student buildStudent(Long studentId, Long groupId) {
        return Student.builder()
                .studentId(studentId)
                .firstName("Ana")
                .lastName("Diaz")
                .birthDate(LocalDate.of(2020, 5, 10))
                .classGroup(ClassGroup.builder().groupId(groupId).name("Group A").build())
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 8, 15))
                .build();
    }

    private User buildUser(Long userId, String email, RoleName roleName) {
        return User.builder()
                .userId(userId)
                .email(email)
                .roles(Set.of(Role.builder().roleId(userId).code(roleName).name(roleName.name()).build()))
                .build();
    }

    private StaffGroupAssignment buildAssignment(Staff staff, ClassGroup group) {
        return StaffGroupAssignment.builder()
                .staffGroupAssignmentId(4L)
                .staff(staff)
                .classGroup(group)
                .roleInGroup(StaffGroupRole.TEACHER)
                .primary(true)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(null)
                .build();
    }
}
