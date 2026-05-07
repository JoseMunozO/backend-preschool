package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentConsentRequest;
import com.preschool.backendpreschool.dto.StudentConsentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsent;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
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
public class StudentConsentService {

    private final StudentConsentRepository studentConsentRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StaffRepository staffRepository;
    private final StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    public List<StudentConsentResponse> getConsents(Long studentId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);

        if (hasInternalAdminRole(requester) || isAssignedTeacher(requester, student)) {
            return studentConsentRepository.findByStudentStudentIdOrderByCreatedAtDesc(studentId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        Parent parent = findRequesterParent(requester);
        ensureParentLinkedToStudent(parent.getParentId(), studentId);

        return studentConsentRepository.findByStudentStudentIdAndParentParentIdOrderByCreatedAtDesc(
                        studentId,
                        parent.getParentId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentConsentResponse acceptConsent(Long studentId, StudentConsentRequest request, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        Parent parent = resolveTargetParent(request, requester, studentId);

        ensureCanWriteConsent(requester, parent.getParentId(), studentId);

        studentConsentRepository.findByStudentStudentIdAndParentParentIdAndConsentTypeAndRevokedAtIsNull(
                studentId,
                parent.getParentId(),
                request.consentType()
        ).ifPresent(existing -> {
            throw new BadRequestException("Ya existe un consentimiento activo de este tipo para este estudiante y tutor");
        });

        StudentConsent consent = StudentConsent.builder()
                .student(student)
                .parent(parent)
                .recordedByUser(requester)
                .consentType(request.consentType())
                .granted(true)
                .notes(trimToNull(request.notes()))
                .acceptedAt(LocalDateTime.now())
                .build();

        return toResponse(studentConsentRepository.save(consent));
    }

    @Transactional
    public StudentConsentResponse revokeConsent(Long studentId, Long consentId, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);
        StudentConsent consent = findConsent(consentId);
        ensureConsentBelongsToStudent(consent, student.getStudentId());

        ensureCanWriteConsent(requester, consent.getParent().getParentId(), studentId);

        consent.setRevokedAt(LocalDateTime.now());
        consent.setGranted(false);

        return toResponse(studentConsentRepository.save(consent));
    }

    private Parent resolveTargetParent(StudentConsentRequest request, User requester, Long studentId) {
        if (hasInternalAdminRole(requester)) {
            if (request.parentId() == null) {
                throw new BadRequestException("parentId es requerido para registrar consentimiento desde administracion");
            }

            Parent parent = parentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor no encontrado"));
            ensureParentLinkedToStudent(parent.getParentId(), studentId);
            return parent;
        }

        Parent parent = findRequesterParent(requester);
        ensureParentLinkedToStudent(parent.getParentId(), studentId);

        if (request.parentId() != null && !request.parentId().equals(parent.getParentId())) {
            throw new ForbiddenException("No tienes permiso para registrar consentimiento de otro tutor");
        }

        return parent;
    }

    private void ensureCanWriteConsent(User requester, Long parentId, Long studentId) {
        if (hasInternalAdminRole(requester)) {
            ensureParentLinkedToStudent(parentId, studentId);
            return;
        }

        Parent parent = findRequesterParent(requester);
        if (parent.getParentId().equals(parentId)) {
            ensureParentLinkedToStudent(parentId, studentId);
            return;
        }

        throw new ForbiddenException("No tienes permiso para gestionar este consentimiento");
    }

    private void ensureParentLinkedToStudent(Long parentId, Long studentId) {
        if (!studentGuardianRepository.existsById(new com.preschool.backendpreschool.model.StudentGuardianId(studentId, parentId))) {
            throw new ForbiddenException("El tutor no esta vinculado a este estudiante");
        }
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Parent findRequesterParent(User requester) {
        return parentRepository.findByUserEmail(requester.getEmail())
                .orElseThrow(() -> new ForbiddenException("No tienes perfil de padre/tutor vinculado"));
    }

    private StudentConsent findConsent(Long consentId) {
        return studentConsentRepository.findById(consentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento no encontrado"));
    }

    private void ensureConsentBelongsToStudent(StudentConsent consent, Long studentId) {
        if (!consent.getStudent().getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Consentimiento no encontrado");
        }
    }

    private boolean isAssignedTeacher(User requester, Student student) {
        if (!hasRole(requester, RoleName.TEACHER)) {
            return false;
        }

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

    private StudentConsentResponse toResponse(StudentConsent consent) {
        Student student = consent.getStudent();
        Parent parent = consent.getParent();
        User recordedBy = consent.getRecordedByUser();

        return new StudentConsentResponse(
                consent.getStudentConsentId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                parent.getParentId(),
                parent.getFirstName() + " " + parent.getLastName(),
                recordedBy.getUserId(),
                recordedBy.getEmail(),
                consent.getConsentType(),
                consent.getGranted(),
                Boolean.TRUE.equals(consent.getGranted()) && consent.getRevokedAt() == null,
                consent.getNotes(),
                consent.getAcceptedAt(),
                consent.getRevokedAt(),
                consent.getCreatedAt(),
                consent.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
