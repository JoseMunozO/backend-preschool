package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentEmergencyContactRequest;
import com.preschool.backendpreschool.dto.StudentEmergencyContactResponse;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentEmergencyContact;
import com.preschool.backendpreschool.repository.StudentEmergencyContactRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentEmergencyContactService {

    private final StudentEmergencyContactRepository studentEmergencyContactRepository;
    private final StudentRepository studentRepository;

    public List<StudentEmergencyContactResponse> getContacts(Long studentId) {
        findStudent(studentId);

        return studentEmergencyContactRepository
                .findByStudentStudentIdOrderByPrimaryDescFullNameAsc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentEmergencyContactResponse createContact(Long studentId, StudentEmergencyContactRequest request) {
        Student student = findStudent(studentId);

        StudentEmergencyContact contact = StudentEmergencyContact.builder()
                .student(student)
                .fullName(request.fullName().trim())
                .relationship(request.relationship().trim())
                .phone(request.phone().trim())
                .alternatePhone(trimToNull(request.alternatePhone()))
                .notes(trimToNull(request.notes()))
                .primary(Boolean.TRUE.equals(request.primary()))
                .build();

        return toResponse(studentEmergencyContactRepository.save(contact));
    }

    @Transactional
    public StudentEmergencyContactResponse updateContact(
            Long studentId,
            Long contactId,
            StudentEmergencyContactRequest request
    ) {
        StudentEmergencyContact contact = findContact(contactId);
        ensureContactBelongsToStudent(contact, studentId);

        contact.setFullName(request.fullName().trim());
        contact.setRelationship(request.relationship().trim());
        contact.setPhone(request.phone().trim());
        contact.setAlternatePhone(trimToNull(request.alternatePhone()));
        contact.setNotes(trimToNull(request.notes()));
        contact.setPrimary(Boolean.TRUE.equals(request.primary()));

        return toResponse(studentEmergencyContactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(Long studentId, Long contactId) {
        StudentEmergencyContact contact = findContact(contactId);
        ensureContactBelongsToStudent(contact, studentId);

        studentEmergencyContactRepository.delete(contact);
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private StudentEmergencyContact findContact(Long contactId) {
        return studentEmergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto de emergencia no encontrado"));
    }

    private void ensureContactBelongsToStudent(StudentEmergencyContact contact, Long studentId) {
        if (!contact.getStudent().getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Contacto de emergencia no encontrado");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private StudentEmergencyContactResponse toResponse(StudentEmergencyContact contact) {
        Student student = contact.getStudent();

        return new StudentEmergencyContactResponse(
                contact.getStudentEmergencyContactId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                contact.getFullName(),
                contact.getRelationship(),
                contact.getPhone(),
                contact.getAlternatePhone(),
                contact.getNotes(),
                contact.getPrimary(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
