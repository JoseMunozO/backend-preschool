package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentGuardianRequest;
import com.preschool.backendpreschool.dto.StudentGuardianResponse;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentGuardian;
import com.preschool.backendpreschool.model.StudentGuardianId;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentGuardianService {

    private final StudentGuardianRepository studentGuardianRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    public List<StudentGuardianResponse> getGuardiansByParent(Long parentId) {
        return studentGuardianRepository.findByParentParentId(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StudentGuardianResponse> getGuardiansByParentEmail(String email) {
        Parent parent = parentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de padre/tutor no encontrado"));

        return getGuardiansByParent(parent.getParentId());
    }

    public List<StudentGuardianResponse> getGuardiansByStudent(Long studentId) {
        return studentGuardianRepository.findByStudentStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentGuardianResponse linkStudent(Long parentId, StudentGuardianRequest request) {
        Parent parent = findParent(parentId);
        Student student = findStudent(request.studentId());
        StudentGuardianId id = new StudentGuardianId(student.getStudentId(), parent.getParentId());

        StudentGuardian guardian = studentGuardianRepository.findById(id)
                .orElseGet(() -> StudentGuardian.builder()
                        .id(id)
                        .parent(parent)
                        .student(student)
                        .build());

        guardian.setRelationshipType(request.relationshipType());
        guardian.setPrimaryContact(request.primaryContact() != null ? request.primaryContact() : false);
        guardian.setBillingContact(request.billingContact() != null ? request.billingContact() : false);
        guardian.setAuthorizedPickup(request.authorizedPickup() != null ? request.authorizedPickup() : true);
        guardian.setLivesWithStudent(request.livesWithStudent() != null ? request.livesWithStudent() : false);

        return toResponse(studentGuardianRepository.save(guardian));
    }

    public void unlinkStudent(Long parentId, Long studentId) {
        StudentGuardianId id = new StudentGuardianId(studentId, parentId);

        if (!studentGuardianRepository.existsById(id)) {
            throw new ResourceNotFoundException("Relacion estudiante-tutor no encontrada");
        }

        studentGuardianRepository.deleteById(id);
    }

    private Parent findParent(Long parentId) {
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor no encontrado"));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private StudentGuardianResponse toResponse(StudentGuardian guardian) {
        Student student = guardian.getStudent();
        Parent parent = guardian.getParent();

        return new StudentGuardianResponse(
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                parent.getParentId(),
                parent.getFirstName() + " " + parent.getLastName(),
                guardian.getRelationshipType(),
                guardian.getPrimaryContact(),
                guardian.getBillingContact(),
                guardian.getAuthorizedPickup(),
                guardian.getLivesWithStudent(),
                guardian.getCreatedAt(),
                guardian.getUpdatedAt()
        );
    }
}
