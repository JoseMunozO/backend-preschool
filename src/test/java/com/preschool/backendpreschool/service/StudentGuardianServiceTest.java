package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentGuardianRequest;
import com.preschool.backendpreschool.dto.StudentGuardianResponse;
import com.preschool.backendpreschool.model.GuardianRelationshipType;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentGuardian;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentGuardianServiceTest {

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentGuardianService studentGuardianService;

    @Test
    void linkStudentCreatesRelationshipWithDefaultFlags() {
        Parent parent = Parent.builder()
                .parentId(5L)
                .firstName("Maria")
                .lastName("Andersson")
                .build();

        Student student = Student.builder()
                .studentId(9L)
                .firstName("Lucas")
                .lastName("Andersson")
                .build();

        StudentGuardianRequest request = new StudentGuardianRequest(
                9L,
                GuardianRelationshipType.MOTHER,
                null,
                null,
                null,
                null
        );

        when(parentRepository.findById(5L)).thenReturn(Optional.of(parent));
        when(studentRepository.findById(9L)).thenReturn(Optional.of(student));
        when(studentGuardianRepository.findById(any())).thenReturn(Optional.empty());
        when(studentGuardianRepository.save(any(StudentGuardian.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentGuardianResponse response = studentGuardianService.linkStudent(5L, request);

        assertThat(response.parentId()).isEqualTo(5L);
        assertThat(response.studentId()).isEqualTo(9L);
        assertThat(response.relationshipType()).isEqualTo(GuardianRelationshipType.MOTHER);
        assertThat(response.primaryContact()).isFalse();
        assertThat(response.billingContact()).isFalse();
        assertThat(response.authorizedPickup()).isTrue();
        assertThat(response.livesWithStudent()).isFalse();
    }
}
