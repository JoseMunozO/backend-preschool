package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.AttendanceReportEntryResponse;
import com.preschool.backendpreschool.dto.StudentAttendanceBulkRequest;
import com.preschool.backendpreschool.dto.StudentAttendanceEntry;
import com.preschool.backendpreschool.dto.StudentAttendanceResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentAttendance;
import com.preschool.backendpreschool.model.StudentAttendanceStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentAttendanceRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @Mock
    private StudentAttendanceRepository studentAttendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    @InjectMocks
    private StudentAttendanceService studentAttendanceService;

    @Test
    void teacherAssignedToGroupCanReadAttendanceRosterWithUnmarkedStudents() {
        LocalDate date = LocalDate.of(2026, 8, 24);
        User teacher = buildUser(RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).build();
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();
        Student luis = Student.builder().studentId(2L).firstName("Luis").lastName("Perez").classGroup(group).build();
        StudentAttendance existing = StudentAttendance.builder()
                .studentAttendanceId(30L)
                .student(luis)
                .attendanceDate(date)
                .status(StudentAttendanceStatus.SICK)
                .recordedByUser(teacher)
                .build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(classGroupRepository.existsById(5L)).thenReturn(true);
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(group, LocalDate.of(2026, 1, 1), null)));
        when(studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(5L)))
                .thenReturn(List.of(ana, luis));
        when(studentAttendanceRepository.findByAttendanceDateAndStudentStudentIdIn(date, List.of(1L, 2L)))
                .thenReturn(List.of(existing));

        List<StudentAttendanceResponse> response = studentAttendanceService.getAttendance(5L, date, "teacher@school.com");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).studentId()).isEqualTo(1L);
        assertThat(response.get(0).status()).isNull();
        assertThat(response.get(1).studentId()).isEqualTo(2L);
        assertThat(response.get(1).status()).isEqualTo(StudentAttendanceStatus.SICK);
    }

    @Test
    void teacherNotAssignedToGroupCannotReadAttendance() {
        User teacher = buildUser(RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(classGroupRepository.existsById(5L)).thenReturn(true);
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentAttendanceService.getAttendance(5L, LocalDate.now(), "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAttendanceForMissingGroupThrowsNotFound() {
        User admin = buildUser(RoleName.ADMIN);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> studentAttendanceService.getAttendance(99L, LocalDate.now(), "admin@school.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveAttendanceCreatesAndUpdatesRecordsForGroupStudentsOnly() {
        LocalDate date = LocalDate.now();
        User admin = buildUser(RoleName.ADMIN);
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(5L)).thenReturn(true);
        when(studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(5L))).thenReturn(List.of(ana));
        when(studentAttendanceRepository.findByStudentStudentIdAndAttendanceDate(1L, date)).thenReturn(Optional.empty());
        when(studentAttendanceRepository.save(any(StudentAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentAttendanceBulkRequest request = new StudentAttendanceBulkRequest(
                5L,
                date,
                List.of(new StudentAttendanceEntry(1L, StudentAttendanceStatus.ABSENT, "  Fiebre  "))
        );

        List<StudentAttendanceResponse> response = studentAttendanceService.saveAttendance(request, "admin@school.com");

        assertThat(response).singleElement().satisfies(entry -> {
            assertThat(entry.studentId()).isEqualTo(1L);
            assertThat(entry.status()).isEqualTo(StudentAttendanceStatus.ABSENT);
            assertThat(entry.notes()).isEqualTo("Fiebre");
        });
    }

    @Test
    void saveAttendanceRejectsStudentNotInGroup() {
        User admin = buildUser(RoleName.ADMIN);
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(5L)).thenReturn(true);
        when(studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(5L))).thenReturn(List.of(ana));

        StudentAttendanceBulkRequest request = new StudentAttendanceBulkRequest(
                5L,
                LocalDate.now(),
                List.of(new StudentAttendanceEntry(999L, StudentAttendanceStatus.PRESENT, null))
        );

        assertThatThrownBy(() -> studentAttendanceService.saveAttendance(request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void saveAttendanceRejectsPastDateAsAlreadyArchived() {
        User admin = buildUser(RoleName.ADMIN);
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(5L)).thenReturn(true);

        StudentAttendanceBulkRequest request = new StudentAttendanceBulkRequest(
                5L,
                LocalDate.now().minusDays(1),
                List.of(new StudentAttendanceEntry(1L, StudentAttendanceStatus.PRESENT, null))
        );

        assertThatThrownBy(() -> studentAttendanceService.saveAttendance(request, "admin@school.com"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void saveAttendanceRejectsFutureDate() {
        User admin = buildUser(RoleName.ADMIN);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(5L)).thenReturn(true);

        StudentAttendanceBulkRequest request = new StudentAttendanceBulkRequest(
                5L,
                LocalDate.now().plusDays(1),
                List.of(new StudentAttendanceEntry(1L, StudentAttendanceStatus.PRESENT, null))
        );

        assertThatThrownBy(() -> studentAttendanceService.saveAttendance(request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getStudentAttendanceHistoryReturnsRecordsForAssignedTeacher() {
        User teacher = buildUser(RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).build();
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();
        StudentAttendance record1 = StudentAttendance.builder()
                .studentAttendanceId(30L)
                .student(ana)
                .attendanceDate(LocalDate.now())
                .status(StudentAttendanceStatus.PRESENT)
                .recordedByUser(teacher)
                .build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(group, LocalDate.of(2026, 1, 1), null)));
        when(studentAttendanceRepository.findByStudentStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(record1));

        List<StudentAttendanceResponse> response = studentAttendanceService.getStudentAttendanceHistory(1L, null, null, "teacher@school.com");

        assertThat(response).singleElement().satisfies(entry -> {
            assertThat(entry.studentAttendanceId()).isEqualTo(30L);
            assertThat(entry.status()).isEqualTo(StudentAttendanceStatus.PRESENT);
        });
    }

    @Test
    void getStudentAttendanceHistoryRejectsTeacherOutsideAssignedGroup() {
        User teacher = buildUser(RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).build();
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentAttendanceService.getStudentAttendanceHistory(1L, null, null, "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getStudentAttendanceHistoryRejectsFromAfterTo() {
        User admin = buildUser(RoleName.ADMIN);
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> studentAttendanceService.getStudentAttendanceHistory(
                1L, LocalDate.now(), LocalDate.now().minusDays(1), "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getStudentAttendanceHistoryForMissingStudentThrowsNotFound() {
        User admin = buildUser(RoleName.ADMIN);

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentAttendanceService.getStudentAttendanceHistory(999L, null, null, "admin@school.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAttendanceReportAggregatesCountsForGroup() {
        User admin = buildUser(RoleName.ADMIN);
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);

        List<StudentAttendance> records = List.of(
                StudentAttendance.builder().student(ana).attendanceDate(from).status(StudentAttendanceStatus.PRESENT).build(),
                StudentAttendance.builder().student(ana).attendanceDate(from.plusDays(1)).status(StudentAttendanceStatus.ABSENT).build()
        );

        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(classGroupRepository.existsById(5L)).thenReturn(true);
        when(studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(5L))).thenReturn(List.of(ana));
        when(studentAttendanceRepository.findByAttendanceDateBetweenAndStudentStudentIdIn(from, to, List.of(1L)))
                .thenReturn(records);

        List<AttendanceReportEntryResponse> report = studentAttendanceService.getAttendanceReport(5L, null, from, to, "admin@school.com");

        assertThat(report).singleElement().satisfies(entry -> {
            assertThat(entry.studentId()).isEqualTo(1L);
            assertThat(entry.presentCount()).isEqualTo(1L);
            assertThat(entry.absentCount()).isEqualTo(1L);
            assertThat(entry.unmarkedCount()).isEqualTo(1L);
        });
    }

    @Test
    void getAttendanceReportDefaultsToTeacherAssignedGroupsWhenGroupIdOmitted() {
        User teacher = buildUser(RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(9L).build();
        ClassGroup group = ClassGroup.builder().groupId(5L).name("Group A").build();
        Student ana = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").classGroup(group).build();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 1);

        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(group, LocalDate.of(2026, 1, 1), null)));
        when(studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(5L))).thenReturn(List.of(ana));
        when(studentAttendanceRepository.findByAttendanceDateBetweenAndStudentStudentIdIn(from, to, List.of(1L)))
                .thenReturn(List.of());

        List<AttendanceReportEntryResponse> report = studentAttendanceService.getAttendanceReport(null, null, from, to, "teacher@school.com");

        assertThat(report).singleElement().satisfies(entry -> assertThat(entry.unmarkedCount()).isEqualTo(1L));
    }

    private StaffGroupAssignment buildAssignment(ClassGroup group, LocalDate startDate, LocalDate endDate) {
        return StaffGroupAssignment.builder()
                .classGroup(group)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private User buildUser(RoleName roleName) {
        Role role = Role.builder().roleId(1L).code(roleName).name(roleName.name()).build();
        return User.builder()
                .userId(1L)
                .email(roleName == RoleName.TEACHER ? "teacher@school.com" : "admin@school.com")
                .roles(Set.of(role))
                .build();
    }
}
