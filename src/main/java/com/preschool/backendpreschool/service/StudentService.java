package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentRequest;
import com.preschool.backendpreschool.dto.StudentProfilePhotoRequest;
import com.preschool.backendpreschool.dto.StudentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsentType;
import com.preschool.backendpreschool.model.StudentGuardian;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StudentConsentRepository studentConsentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public List<StudentResponse> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        List<Long> studentIds = students.stream()
                .map(Student::getStudentId)
                .toList();
        Map<Long, String> primaryGuardianNames = studentIds.isEmpty()
                ? Map.of()
                : studentGuardianRepository.findByStudentStudentIdInAndPrimaryContactTrue(studentIds)
                .stream()
                .collect(Collectors.toMap(
                        guardian -> guardian.getStudent().getStudentId(),
                        this::formatGuardianName,
                        (first, ignored) -> first
                ));

        return students.stream()
                .map(student -> mapToResponse(student, primaryGuardianNames.get(student.getStudentId())))
                .toList();
    }

    public StudentResponse getStudentById(Long id) {
        Student student = findStudentOrThrow(id);
        return mapToResponse(student);
    }

    public StudentResponse createStudent(StudentRequest request) {
        if (request.studentCode() != null && studentRepository.existsByStudentCode(request.studentCode())) {
            throw new BadRequestException("Ya existe un estudiante con ese código");
        }

        Student student = Student.builder()
                .studentCode(request.studentCode())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .birthDate(request.birthDate())
                .classGroup(findClassGroup(request.groupId()))
                .status(request.status() != null ? request.status() : StudentStatus.active)
                .enrollmentDate(request.enrollmentDate())
                .withdrawalDate(request.withdrawalDate())
                .medicalNotes(request.medicalNotes())
                .allergies(request.allergies())
                .notes(request.notes())
                .build();

        Student savedStudent = studentRepository.save(student);
        return mapToResponse(savedStudent);
    }

    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = findStudentOrThrow(id);

        if (request.studentCode() != null &&
                !request.studentCode().equals(student.getStudentCode()) &&
                studentRepository.existsByStudentCode(request.studentCode())) {
            throw new BadRequestException("Ya existe un estudiante con ese código");
        }

        student.setStudentCode(request.studentCode());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setBirthDate(request.birthDate());
        student.setClassGroup(findClassGroup(request.groupId()));
        student.setStatus(request.status() != null ? request.status() : StudentStatus.active);
        student.setEnrollmentDate(request.enrollmentDate());
        student.setWithdrawalDate(request.withdrawalDate());
        student.setMedicalNotes(request.medicalNotes());
        student.setAllergies(request.allergies());
        student.setNotes(request.notes());

        Student updatedStudent = studentRepository.save(student);
        return mapToResponse(updatedStudent);
    }

    public void deleteStudent(Long id) {
        Student student = findStudentOrThrow(id);
        studentRepository.delete(student);
    }

    public StudentResponse updateProfilePhoto(Long id, StudentProfilePhotoRequest request) {
        Student student = findStudentOrThrow(id);
        if (!studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                id,
                StudentConsentType.IMAGE_PROFILE_PHOTO
        )) {
            throw new BadRequestException("Se requiere consentimiento activo de imagen para asignar foto de perfil");
        }

        student.setProfilePhotoUrl(request.profilePhotoUrl().trim());

        return mapToResponse(studentRepository.save(student));
    }

    public StudentResponse removeProfilePhoto(Long id) {
        Student student = findStudentOrThrow(id);
        student.setProfilePhotoUrl(null);

        return mapToResponse(studentRepository.save(student));
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private ClassGroup findClassGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }

        return classGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
    }

    private StudentResponse mapToResponse(Student student) {
        String primaryGuardianName = studentGuardianRepository
                .findFirstByStudentStudentIdAndPrimaryContactTrue(student.getStudentId())
                .map(this::formatGuardianName)
                .orElse(null);

        return mapToResponse(student, primaryGuardianName);
    }

    private StudentResponse mapToResponse(Student student, String primaryGuardianName) {
        ClassGroup group = student.getClassGroup();

        return new StudentResponse(
                student.getStudentId(),
                student.getStudentCode(),
                student.getFirstName(),
                student.getLastName(),
                student.getProfilePhotoUrl(),
                student.getBirthDate(),
                group != null ? group.getGroupId() : null,
                group != null ? group.getName() : null,
                primaryGuardianName,
                student.getStatus(),
                student.getEnrollmentDate(),
                student.getWithdrawalDate(),
                student.getMedicalNotes(),
                student.getAllergies(),
                student.getNotes(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private String formatGuardianName(StudentGuardian guardian) {
        return guardian.getParent().getFirstName() + " " + guardian.getParent().getLastName();
    }
}
