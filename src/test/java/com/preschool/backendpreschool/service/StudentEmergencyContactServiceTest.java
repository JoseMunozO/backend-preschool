package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentEmergencyContactRequest;
import com.preschool.backendpreschool.dto.StudentEmergencyContactResponse;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentEmergencyContact;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.StudentEmergencyContactRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentEmergencyContactServiceTest {

    @Mock
    private StudentEmergencyContactRepository studentEmergencyContactRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentEmergencyContactService studentEmergencyContactService;

    @Test
    void createContactTrimsFieldsAndDefaultsPrimaryToFalse() {
        Student student = buildStudent();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentEmergencyContactRepository.save(any(StudentEmergencyContact.class)))
                .thenAnswer(invocation -> {
                    StudentEmergencyContact contact = invocation.getArgument(0);
                    contact.setStudentEmergencyContactId(10L);
                    return contact;
                });

        StudentEmergencyContactResponse response = studentEmergencyContactService.createContact(
                1L,
                new StudentEmergencyContactRequest("  Maria Lopez  ", " Vecina ", " +46000000099 ", null, "  ", null)
        );

        assertThat(response.studentEmergencyContactId()).isEqualTo(10L);
        assertThat(response.fullName()).isEqualTo("Maria Lopez");
        assertThat(response.relationship()).isEqualTo("Vecina");
        assertThat(response.phone()).isEqualTo("+46000000099");
        assertThat(response.notes()).isNull();
        assertThat(response.primary()).isFalse();
        assertThat(response.studentName()).isEqualTo("Ana Diaz");
    }

    @Test
    void createContactThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentEmergencyContactService.createContact(
                99L,
                new StudentEmergencyContactRequest("Maria Lopez", "Vecina", "+46000000099", null, null, null)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estudiante no encontrado");
    }

    @Test
    void updateContactRejectsContactFromAnotherStudent() {
        Student student = buildStudent();
        Student otherStudent = Student.builder().studentId(2L).build();
        StudentEmergencyContact contact = StudentEmergencyContact.builder()
                .studentEmergencyContactId(10L)
                .student(otherStudent)
                .build();

        when(studentEmergencyContactRepository.findById(10L)).thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> studentEmergencyContactService.updateContact(
                1L,
                10L,
                new StudentEmergencyContactRequest("Maria Lopez", "Vecina", "+46000000099", null, null, null)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Contacto de emergencia no encontrado");
    }

    @Test
    void getContactsReturnsThemOrderedByRepository() {
        Student student = buildStudent();
        StudentEmergencyContact contact = StudentEmergencyContact.builder()
                .studentEmergencyContactId(10L)
                .student(student)
                .fullName("Maria Lopez")
                .relationship("Vecina")
                .phone("+46000000099")
                .primary(true)
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentEmergencyContactRepository.findByStudentStudentIdOrderByPrimaryDescFullNameAsc(1L))
                .thenReturn(List.of(contact));

        List<StudentEmergencyContactResponse> response = studentEmergencyContactService.getContacts(1L);

        assertThat(response).singleElement()
                .extracting(StudentEmergencyContactResponse::fullName)
                .isEqualTo("Maria Lopez");
    }

    private Student buildStudent() {
        return Student.builder()
                .studentId(1L)
                .firstName("Ana")
                .lastName("Diaz")
                .birthDate(LocalDate.of(2020, 5, 10))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2024, 8, 15))
                .build();
    }
}
