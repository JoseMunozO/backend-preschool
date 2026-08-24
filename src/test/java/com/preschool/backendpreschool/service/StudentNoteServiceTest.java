package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentNoteAuditLogResponse;
import com.preschool.backendpreschool.dto.StudentNoteHistoryEntryResponse;
import com.preschool.backendpreschool.dto.StudentNoteRequest;
import com.preschool.backendpreschool.dto.StudentNoteResponse;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.StaffGroupRole;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentNote;
import com.preschool.backendpreschool.model.StudentNoteAuditLog;
import com.preschool.backendpreschool.model.StudentNoteType;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentNoteAuditLogRepository;
import com.preschool.backendpreschool.repository.StudentNoteRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentNoteServiceTest {

    @Mock
    private StudentNoteRepository studentNoteRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    @Mock
    private StudentNoteAuditLogRepository studentNoteAuditLogRepository;

    @InjectMocks
    private StudentNoteService studentNoteService;

    @Test
    void adminCanCreateModeratedNoteForAnyStudent() {
        Student student = buildStudent(1L, 10L);
        User admin = buildUser(2L, "admin@school.com", RoleName.ADMIN);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> {
            StudentNote note = invocation.getArgument(0);
            note.setStudentNoteId(20L);
            return note;
        });

        StudentNoteResponse response = studentNoteService.createNote(
                1L,
                new StudentNoteRequest(StudentNoteType.PEDAGOGICAL, "  Needs extra reading support.  "),
                "admin@school.com"
        );

        assertThat(response.studentNoteId()).isEqualTo(20L);
        assertThat(response.content()).isEqualTo("Needs extra reading support.");
        assertThat(response.moderated()).isTrue();
    }

    @Test
    void assignedTeacherCanCreateUnmoderatedNoteForOwnGroupStudent() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L))
                .thenReturn(List.of(buildAssignment(staff, student.getClassGroup())));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentNoteResponse response = studentNoteService.createNote(
                1L,
                new StudentNoteRequest(StudentNoteType.BEHAVIOR, "Positive participation today."),
                "teacher@school.com"
        );

        assertThat(response.content()).isEqualTo("Positive participation today.");
        assertThat(response.moderated()).isFalse();
    }

    @Test
    void teacherCannotCreateNoteForStudentOutsideAssignedGroups() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentNoteService.createNote(
                1L,
                new StudentNoteRequest(StudentNoteType.INCIDENT, "Incident note."),
                "teacher@school.com"
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No tienes permiso para gestionar notas de este estudiante");
    }

    @Test
    void parentCannotReadStudentNotes() {
        Student student = buildStudent(1L, 10L);
        User parent = buildUser(4L, "parent@school.com", RoleName.PARENT);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("parent@school.com")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> studentNoteService.getNotes(1L, "parent@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignedTeacherCanModerateNoteForOwnGroupStudent() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(teacher)
                .noteType(StudentNoteType.BEHAVIOR)
                .content("Needs review")
                .moderated(false)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L))
                .thenReturn(List.of(buildAssignment(staff, student.getClassGroup())));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentNoteResponse response = studentNoteService.moderateNote(1L, 8L, "teacher@school.com");

        assertThat(response.moderated()).isTrue();
        assertThat(note.getModerated()).isTrue();
    }

    @Test
    void deleteNoteUsesSoftDelete() {
        Student student = buildStudent(1L, 10L);
        User director = buildUser(5L, "director@school.com", RoleName.DIRECTOR);
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(director)
                .noteType(StudentNoteType.ADMINISTRATIVE)
                .content("Follow up")
                .moderated(true)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentNoteService.deleteNote(1L, 8L, "director@school.com");

        assertThat(note.getDeleted()).isTrue();
        assertThat(note.getDeletedAt()).isNotNull();
    }

    @Test
    void assignedTeacherCanUpdateOwnNote() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(teacher)
                .noteType(StudentNoteType.BEHAVIOR)
                .content("Original content")
                .moderated(false)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L))
                .thenReturn(List.of(buildAssignment(staff, student.getClassGroup())));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentNoteResponse response = studentNoteService.updateNote(
                1L,
                8L,
                new StudentNoteRequest(StudentNoteType.BEHAVIOR, "Updated by author"),
                "teacher@school.com"
        );

        assertThat(response.content()).isEqualTo("Updated by author");
    }

    @Test
    void teacherCannotUpdateAnotherTeachersNote() {
        Student student = buildStudent(1L, 10L);
        User author = buildUser(3L, "author-teacher@school.com", RoleName.TEACHER);
        User otherTeacher = buildUser(6L, "other-teacher@school.com", RoleName.TEACHER);
        Staff otherStaff = Staff.builder().staffId(9L).user(otherTeacher).build();
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(author)
                .noteType(StudentNoteType.BEHAVIOR)
                .content("Original content")
                .moderated(false)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("other-teacher@school.com")).thenReturn(Optional.of(otherTeacher));
        when(staffRepository.findByUserEmail("other-teacher@school.com")).thenReturn(Optional.of(otherStaff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(otherStaff, student.getClassGroup())));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> studentNoteService.updateNote(
                1L,
                8L,
                new StudentNoteRequest(StudentNoteType.BEHAVIOR, "Trying to edit someone else's note"),
                "other-teacher@school.com"
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Solo puedes editar o eliminar las notas que tu creaste");
    }

    @Test
    void teacherCannotDeleteAnotherTeachersNote() {
        Student student = buildStudent(1L, 10L);
        User author = buildUser(3L, "author-teacher@school.com", RoleName.TEACHER);
        User otherTeacher = buildUser(6L, "other-teacher@school.com", RoleName.TEACHER);
        Staff otherStaff = Staff.builder().staffId(9L).user(otherTeacher).build();
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(author)
                .noteType(StudentNoteType.BEHAVIOR)
                .content("Original content")
                .moderated(false)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("other-teacher@school.com")).thenReturn(Optional.of(otherTeacher));
        when(staffRepository.findByUserEmail("other-teacher@school.com")).thenReturn(Optional.of(otherStaff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(9L))
                .thenReturn(List.of(buildAssignment(otherStaff, student.getClassGroup())));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> studentNoteService.deleteNote(1L, 8L, "other-teacher@school.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Solo puedes editar o eliminar las notas que tu creaste");
    }

    @Test
    void directorCanUpdateNoteAuthoredByATeacher() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        User director = buildUser(5L, "director@school.com", RoleName.DIRECTOR);
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(teacher)
                .noteType(StudentNoteType.BEHAVIOR)
                .content("Original content")
                .moderated(false)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentNoteResponse response = studentNoteService.updateNote(
                1L,
                8L,
                new StudentNoteRequest(StudentNoteType.ADMINISTRATIVE, "Updated by director"),
                "director@school.com"
        );

        assertThat(response.content()).isEqualTo("Updated by director");
        assertThat(response.moderated()).isTrue();
    }

    @Test
    void updateNoteRecordsAuditLogWithPreviousAndNewValues() {
        Student student = buildStudent(1L, 10L);
        User director = buildUser(5L, "director@school.com", RoleName.DIRECTOR);
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(director)
                .noteType(StudentNoteType.ADMINISTRATIVE)
                .content("Original content")
                .moderated(true)
                .deleted(false)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteRepository.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentNoteResponse response = studentNoteService.updateNote(
                1L,
                8L,
                new StudentNoteRequest(StudentNoteType.BEHAVIOR, "Updated content"),
                "director@school.com"
        );

        assertThat(response.content()).isEqualTo("Updated content");

        ArgumentCaptor<StudentNoteAuditLog> captor = ArgumentCaptor.forClass(StudentNoteAuditLog.class);
        verify(studentNoteAuditLogRepository).save(captor.capture());

        StudentNoteAuditLog savedAuditLog = captor.getValue();
        assertThat(savedAuditLog.getStudentNote()).isEqualTo(note);
        assertThat(savedAuditLog.getChangedByUser()).isEqualTo(director);
        assertThat(savedAuditLog.getPreviousValues())
                .isEqualTo("noteType=ADMINISTRATIVE; content=Original content");
        assertThat(savedAuditLog.getNewValues())
                .isEqualTo("noteType=BEHAVIOR; content=Updated content");
    }

    @Test
    void getAuditLogReturnsRecordedEntriesForAuthorizedUser() {
        Student student = buildStudent(1L, 10L);
        User director = buildUser(5L, "director@school.com", RoleName.DIRECTOR);
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(director)
                .noteType(StudentNoteType.ADMINISTRATIVE)
                .content("Updated content")
                .moderated(true)
                .deleted(false)
                .build();
        StudentNoteAuditLog auditLog = StudentNoteAuditLog.builder()
                .studentNoteAuditLogId(30L)
                .studentNote(note)
                .changedByUser(director)
                .previousValues("noteType=ADMINISTRATIVE; content=Original content")
                .newValues("noteType=ADMINISTRATIVE; content=Updated content")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(studentNoteRepository.findById(8L)).thenReturn(Optional.of(note));
        when(studentNoteAuditLogRepository.findByStudentNoteStudentNoteIdOrderByChangedAtDesc(8L))
                .thenReturn(List.of(auditLog));

        List<StudentNoteAuditLogResponse> response = studentNoteService.getAuditLog(1L, 8L, "director@school.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).studentNoteAuditLogId()).isEqualTo(30L);
        assertThat(response.get(0).changedByEmail()).isEqualTo("director@school.com");
        assertThat(response.get(0).previousValues()).isEqualTo("noteType=ADMINISTRATIVE; content=Original content");
        assertThat(response.get(0).newValues()).isEqualTo("noteType=ADMINISTRATIVE; content=Updated content");
    }

    @Test
    void getAuditLogRejectsTeacherOutsideAssignedGroup() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentNoteService.getAuditLog(1L, 8L, "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getNotesHistoryNestsAuditLogUnderEachNote() {
        Student student = buildStudent(1L, 10L);
        User director = buildUser(5L, "director@school.com", RoleName.DIRECTOR);
        StudentNote note = StudentNote.builder()
                .studentNoteId(8L)
                .student(student)
                .authorUser(director)
                .noteType(StudentNoteType.HEALTH)
                .content("Allergic reaction observed")
                .moderated(true)
                .deleted(false)
                .build();
        StudentNoteAuditLog auditLog = StudentNoteAuditLog.builder()
                .studentNoteAuditLogId(30L)
                .studentNote(note)
                .changedByUser(director)
                .previousValues("noteType=HEALTH; content=Original")
                .newValues("noteType=HEALTH; content=Allergic reaction observed")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("director@school.com")).thenReturn(Optional.of(director));
        when(studentNoteRepository.findByStudentStudentIdAndDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(note));
        when(studentNoteAuditLogRepository.findByStudentNoteStudentNoteIdOrderByChangedAtDesc(8L))
                .thenReturn(List.of(auditLog));

        List<StudentNoteHistoryEntryResponse> history = studentNoteService.getNotesHistory(1L, "director@school.com");

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.studentNoteId()).isEqualTo(8L);
            assertThat(entry.content()).isEqualTo("Allergic reaction observed");
            assertThat(entry.auditLog()).singleElement().satisfies(logEntry ->
                    assertThat(logEntry.previousValues()).isEqualTo("noteType=HEALTH; content=Original"));
        });
    }

    @Test
    void getNotesHistoryRejectsTeacherOutsideAssignedGroup() {
        Student student = buildStudent(1L, 10L);
        User teacher = buildUser(3L, "teacher@school.com", RoleName.TEACHER);
        Staff staff = Staff.builder().staffId(7L).user(teacher).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("teacher@school.com")).thenReturn(Optional.of(teacher));
        when(staffRepository.findByUserEmail("teacher@school.com")).thenReturn(Optional.of(staff));
        when(staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> studentNoteService.getNotesHistory(1L, "teacher@school.com"))
                .isInstanceOf(ForbiddenException.class);
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
