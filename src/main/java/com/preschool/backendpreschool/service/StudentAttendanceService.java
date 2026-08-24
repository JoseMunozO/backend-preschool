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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    public List<StudentAttendanceResponse> getAttendance(Long groupId, LocalDate date, String requesterEmail) {
        User requester = findUser(requesterEmail);
        ensureCanAccessGroup(requester, groupId);

        List<Student> students = studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(groupId));
        Map<Long, StudentAttendance> byStudentId = studentAttendanceRepository
                .findByAttendanceDateAndStudentStudentIdIn(date, students.stream().map(Student::getStudentId).toList())
                .stream()
                .collect(Collectors.toMap(attendance -> attendance.getStudent().getStudentId(), attendance -> attendance));

        return students.stream()
                .sorted(Comparator.comparing(Student::getFirstName).thenComparing(Student::getLastName))
                .map(student -> toResponse(student, date, byStudentId.get(student.getStudentId())))
                .toList();
    }

    private static final int DEFAULT_HISTORY_WINDOW_DAYS = 30;

    public List<StudentAttendanceResponse> getStudentAttendanceHistory(Long studentId, LocalDate from, LocalDate to, String requesterEmail) {
        User requester = findUser(requesterEmail);
        Student student = studentRepository.findByStudentIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        ensureCanAccessStudentAttendance(requester, student);

        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_HISTORY_WINDOW_DAYS - 1);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'");
        }

        return studentAttendanceRepository
                .findByStudentStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(studentId, effectiveFrom, effectiveTo)
                .stream()
                .map(attendance -> toResponse(student, attendance.getAttendanceDate(), attendance))
                .toList();
    }

    public List<AttendanceReportEntryResponse> getAttendanceReport(
            Long groupId, Long studentId, LocalDate from, LocalDate to, String requesterEmail
    ) {
        User requester = findUser(requesterEmail);
        List<Long> groupIds = resolveAccessibleGroupIds(requester, groupId);

        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_HISTORY_WINDOW_DAYS - 1);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'");
        }

        List<Student> students = groupIds.isEmpty()
                ? List.of()
                : studentRepository.findAllByClassGroupGroupIdInAndDeletedAtIsNull(groupIds).stream()
                        .filter(student -> studentId == null || student.getStudentId().equals(studentId))
                        .toList();

        List<Long> studentIds = students.stream().map(Student::getStudentId).toList();
        Map<Long, List<StudentAttendance>> attendanceByStudent = studentIds.isEmpty()
                ? Map.of()
                : studentAttendanceRepository
                        .findByAttendanceDateBetweenAndStudentStudentIdIn(effectiveFrom, effectiveTo, studentIds)
                        .stream()
                        .collect(Collectors.groupingBy(attendance -> attendance.getStudent().getStudentId()));

        long totalDays = effectiveTo.toEpochDay() - effectiveFrom.toEpochDay() + 1;

        return students.stream()
                .sorted(Comparator.comparing(Student::getFirstName).thenComparing(Student::getLastName))
                .map(student -> toReportEntry(student, attendanceByStudent.getOrDefault(student.getStudentId(), List.of()), totalDays))
                .toList();
    }

    private List<Long> resolveAccessibleGroupIds(User requester, Long groupId) {
        if (groupId != null) {
            ensureCanAccessGroup(requester, groupId);
            return List.of(groupId);
        }
        if (hasInternalAdminRole(requester)) {
            return classGroupRepository.findAll().stream().map(ClassGroup::getGroupId).toList();
        }
        if (hasRole(requester, RoleName.TEACHER)) {
            return assignedGroupIds(requester);
        }
        throw new ForbiddenException("No tienes permiso para consultar este reporte");
    }

    private List<Long> assignedGroupIds(User requester) {
        LocalDate today = LocalDate.now();
        return staffRepository.findByUserEmail(requester.getEmail())
                .map(Staff::getStaffId)
                .map(staffId -> staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(staffId)
                        .stream()
                        .filter(assignment -> isActiveAssignment(assignment, today))
                        .map(assignment -> assignment.getClassGroup().getGroupId())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    private AttendanceReportEntryResponse toReportEntry(Student student, List<StudentAttendance> records, long totalDays) {
        Map<StudentAttendanceStatus, Long> counts = records.stream()
                .collect(Collectors.groupingBy(StudentAttendance::getStatus, Collectors.counting()));
        ClassGroup group = student.getClassGroup();

        return new AttendanceReportEntryResponse(
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                group != null ? group.getGroupId() : null,
                group != null ? group.getName() : null,
                counts.getOrDefault(StudentAttendanceStatus.PRESENT, 0L),
                counts.getOrDefault(StudentAttendanceStatus.ABSENT, 0L),
                counts.getOrDefault(StudentAttendanceStatus.LATE, 0L),
                counts.getOrDefault(StudentAttendanceStatus.SICK, 0L),
                totalDays - records.size()
        );
    }

    @Transactional
    public List<StudentAttendanceResponse> saveAttendance(StudentAttendanceBulkRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        ensureCanAccessGroup(requester, request.groupId());
        ensureDateIsEditable(request.date());

        Map<Long, Student> groupStudentsById = studentRepository
                .findAllByClassGroupGroupIdInAndDeletedAtIsNull(List.of(request.groupId()))
                .stream()
                .collect(Collectors.toMap(Student::getStudentId, student -> student));

        List<StudentAttendanceResponse> responses = new ArrayList<>();
        for (StudentAttendanceEntry entry : request.records()) {
            Student student = groupStudentsById.get(entry.studentId());
            if (student == null) {
                throw new BadRequestException("El estudiante " + entry.studentId() + " no pertenece al grupo indicado");
            }

            StudentAttendance attendance = studentAttendanceRepository
                    .findByStudentStudentIdAndAttendanceDate(entry.studentId(), request.date())
                    .orElseGet(() -> StudentAttendance.builder()
                            .student(student)
                            .attendanceDate(request.date())
                            .build());

            attendance.setStatus(entry.status());
            attendance.setNotes(trimToNull(entry.notes()));
            attendance.setRecordedByUser(requester);

            responses.add(toResponse(student, request.date(), studentAttendanceRepository.save(attendance)));
        }

        return responses;
    }

    private void ensureDateIsEditable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ConflictException("No se puede modificar la asistencia de un dia anterior; queda archivado a partir de la medianoche");
        }
        if (date.isAfter(today)) {
            throw new BadRequestException("No se puede registrar asistencia de un dia futuro");
        }
    }

    private void ensureCanAccessStudentAttendance(User requester, Student student) {
        if (hasInternalAdminRole(requester)) {
            return;
        }

        Long groupId = student.getClassGroup() != null ? student.getClassGroup().getGroupId() : null;
        if (groupId != null && hasRole(requester, RoleName.TEACHER) && isAssignedToGroup(requester, groupId)) {
            return;
        }

        throw new ForbiddenException("No tienes permiso para consultar la asistencia de este estudiante");
    }

    private void ensureCanAccessGroup(User requester, Long groupId) {
        if (!classGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo no encontrado");
        }
        if (hasInternalAdminRole(requester)) {
            return;
        }
        if (hasRole(requester, RoleName.TEACHER) && isAssignedToGroup(requester, groupId)) {
            return;
        }
        throw new ForbiddenException("No tienes permiso para gestionar asistencia de este grupo");
    }

    private boolean isAssignedToGroup(User requester, Long groupId) {
        LocalDate today = LocalDate.now();
        return staffRepository.findByUserEmail(requester.getEmail())
                .map(Staff::getStaffId)
                .map(staffId -> staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(staffId)
                        .stream()
                        .filter(assignment -> assignment.getClassGroup().getGroupId().equals(groupId))
                        .anyMatch(assignment -> isActiveAssignment(assignment, today)))
                .orElse(false);
    }

    private boolean isActiveAssignment(StaffGroupAssignment assignment, LocalDate today) {
        return !assignment.getStartDate().isAfter(today)
                && (assignment.getEndDate() == null || !assignment.getEndDate().isBefore(today));
    }

    private boolean hasInternalAdminRole(User user) {
        return hasRole(user, RoleName.SUPER_ADMIN) || hasRole(user, RoleName.ADMIN) || hasRole(user, RoleName.DIRECTOR);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getCode() == roleName);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private StudentAttendanceResponse toResponse(Student student, LocalDate date, StudentAttendance attendance) {
        User recordedBy = attendance != null ? attendance.getRecordedByUser() : null;

        return new StudentAttendanceResponse(
                attendance != null ? attendance.getStudentAttendanceId() : null,
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                date,
                attendance != null ? attendance.getStatus() : null,
                attendance != null ? attendance.getNotes() : null,
                recordedBy != null ? recordedBy.getUserId() : null,
                recordedBy != null ? recordedBy.getEmail() : null,
                attendance != null ? attendance.getCreatedAt() : null,
                attendance != null ? attendance.getUpdatedAt() : null
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
