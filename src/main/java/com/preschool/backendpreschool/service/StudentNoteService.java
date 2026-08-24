package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentNoteAuditLogResponse;
import com.preschool.backendpreschool.dto.StudentNoteRequest;
import com.preschool.backendpreschool.dto.StudentNoteResponse;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentNote;
import com.preschool.backendpreschool.model.StudentNoteAuditLog;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentNoteAuditLogRepository;
import com.preschool.backendpreschool.repository.StudentNoteRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentNoteService {

    private final StudentNoteRepository studentNoteRepository;
    private final StudentNoteAuditLogRepository studentNoteAuditLogRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    public List<StudentNoteResponse> getNotes(Long studentId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        return studentNoteRepository.findByStudentStudentIdAndDeletedFalseOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentNoteResponse createNote(Long studentId, StudentNoteRequest request, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        StudentNote note = StudentNote.builder()
                .student(student)
                .authorUser(requester)
                .noteType(request.noteType())
                .content(request.content().trim())
                .moderated(hasInternalAdminRole(requester))
                .deleted(false)
                .build();

        return toResponse(studentNoteRepository.save(note));
    }

    @Transactional
    public StudentNoteResponse updateNote(Long studentId, Long noteId, StudentNoteRequest request, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        StudentNote note = findNote(noteId);
        ensureNoteBelongsToStudent(note, studentId);

        String previousValues = snapshotNote(note);

        note.setNoteType(request.noteType());
        note.setContent(request.content().trim());
        if (hasInternalAdminRole(requester)) {
            note.setModerated(true);
        }

        StudentNote savedNote = studentNoteRepository.save(note);
        recordAuditLog(savedNote, previousValues, snapshotNote(savedNote), requester);

        return toResponse(savedNote);
    }

    public List<StudentNoteAuditLogResponse> getAuditLog(Long studentId, Long noteId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        StudentNote note = findNote(noteId);
        ensureNoteBelongsToStudent(note, studentId);

        return studentNoteAuditLogRepository.findByStudentNoteStudentNoteIdOrderByChangedAtDesc(noteId)
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional
    public StudentNoteResponse moderateNote(Long studentId, Long noteId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        StudentNote note = findNote(noteId);
        ensureNoteBelongsToStudent(note, studentId);

        note.setModerated(true);

        return toResponse(studentNoteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Long studentId, Long noteId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        ensureCanAccessStudentNotes(requester, student);

        StudentNote note = findNote(noteId);
        ensureNoteBelongsToStudent(note, studentId);

        note.setDeleted(true);
        note.setDeletedAt(LocalDateTime.now());
        studentNoteRepository.save(note);
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private StudentNote findNote(Long noteId) {
        StudentNote note = studentNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));

        if (Boolean.TRUE.equals(note.getDeleted())) {
            throw new ResourceNotFoundException("Nota no encontrada");
        }

        return note;
    }

    private void ensureNoteBelongsToStudent(StudentNote note, Long studentId) {
        if (!note.getStudent().getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Nota no encontrada");
        }
    }

    private void ensureCanAccessStudentNotes(User requester, Student student) {
        if (hasInternalAdminRole(requester)) {
            return;
        }

        if (hasRole(requester, RoleName.TEACHER) && isAssignedToStudentGroup(requester, student)) {
            return;
        }

        throw new ForbiddenException("No tienes permiso para gestionar notas de este estudiante");
    }

    private boolean isAssignedToStudentGroup(User requester, Student student) {
        ClassGroup group = student.getClassGroup();
        if (group == null) {
            return false;
        }

        return staffRepository.findByUserEmail(requester.getEmail())
                .map(staff -> isAssignedToGroup(staff, group.getGroupId()))
                .orElse(false);
    }

    private boolean isAssignedToGroup(Staff staff, Long groupId) {
        LocalDate today = LocalDate.now();
        return staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(staff.getStaffId())
                .stream()
                .filter(assignment -> assignment.getClassGroup().getGroupId().equals(groupId))
                .anyMatch(assignment -> isActiveAssignment(assignment, today));
    }

    private boolean isActiveAssignment(StaffGroupAssignment assignment, LocalDate today) {
        return !assignment.getStartDate().isAfter(today)
                && (assignment.getEndDate() == null || !assignment.getEndDate().isBefore(today));
    }

    private boolean hasInternalAdminRole(User user) {
        return hasRole(user, RoleName.SUPER_ADMIN)
                || hasRole(user, RoleName.ADMIN)
                || hasRole(user, RoleName.DIRECTOR);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.getCode() == roleName);
    }

    private void recordAuditLog(StudentNote note, String previousValues, String newValues, User changedBy) {
        StudentNoteAuditLog auditLog = StudentNoteAuditLog.builder()
                .studentNote(note)
                .changedByUser(changedBy)
                .previousValues(previousValues)
                .newValues(newValues)
                .build();

        studentNoteAuditLogRepository.save(auditLog);
    }

    private String snapshotNote(StudentNote note) {
        return "noteType=" + note.getNoteType() + "; content=" + note.getContent();
    }

    private StudentNoteAuditLogResponse toAuditLogResponse(StudentNoteAuditLog auditLog) {
        User changedBy = auditLog.getChangedByUser();

        return new StudentNoteAuditLogResponse(
                auditLog.getStudentNoteAuditLogId(),
                auditLog.getStudentNote().getStudentNoteId(),
                changedBy != null ? changedBy.getUserId() : null,
                changedBy != null ? changedBy.getEmail() : null,
                auditLog.getChangedAt(),
                auditLog.getPreviousValues(),
                auditLog.getNewValues()
        );
    }

    private StudentNoteResponse toResponse(StudentNote note) {
        Student student = note.getStudent();
        User author = note.getAuthorUser();

        return new StudentNoteResponse(
                note.getStudentNoteId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                author.getUserId(),
                author.getEmail(),
                note.getNoteType(),
                note.getContent(),
                note.getModerated(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
