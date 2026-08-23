package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentDiscountRequest;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentDiscount;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.StudentDiscountRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDiscountServiceTest {

    @Mock
    private StudentDiscountRepository studentDiscountRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudentDiscountService studentDiscountService;

    @Test
    void createDiscountSucceedsWithValidRequest() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentDiscountRepository.save(any(StudentDiscount.class))).thenAnswer(invocation -> {
            StudentDiscount discount = invocation.getArgument(0);
            discount.setStudentDiscountId(5L);
            return discount;
        });

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, new BigDecimal("10"), "Hermanos", LocalDate.of(2026, 8, 1), null
        );

        StudentDiscountResponse response = studentDiscountService.createDiscount(1L, request, "admin@school.com");

        assertThat(response.studentDiscountId()).isEqualTo(5L);
        assertThat(response.active()).isTrue();
        assertThat(response.reason()).isEqualTo("Hermanos");
    }

    @Test
    void createDiscountRejectsPercentageAboveOneHundred() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, new BigDecimal("150"), "Beca", LocalDate.of(2026, 8, 1), null
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(1L, request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDiscountRejectsValidUntilBeforeValidFrom() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.FIXED_AMOUNT, new BigDecimal("50"), "Referido", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(1L, request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDiscountForMissingStudentThrowsNotFound() {
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.FIXED_AMOUNT, new BigDecimal("50"), "Referido", LocalDate.of(2026, 8, 1), null
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(99L, request, "admin@school.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateDiscountMarksInactive() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        StudentDiscount discount = StudentDiscount.builder()
                .studentDiscountId(5L)
                .student(student)
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .validFrom(LocalDate.of(2026, 8, 1))
                .active(true)
                .build();

        when(studentDiscountRepository.findById(5L)).thenReturn(Optional.of(discount));
        when(studentDiscountRepository.save(any(StudentDiscount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDiscountResponse response = studentDiscountService.deactivateDiscount(1L, 5L);

        assertThat(response.active()).isFalse();
    }

    @Test
    void findEffectiveDiscountReturnsMostRecentlyStartedWhenMultipleValid() {
        Student student = Student.builder().studentId(1L).build();
        StudentDiscount older = StudentDiscount.builder()
                .studentDiscountId(1L).student(student).discountType(DiscountType.FIXED_AMOUNT)
                .value(BigDecimal.TEN).validFrom(LocalDate.of(2026, 1, 1)).validUntil(null).active(true).build();
        StudentDiscount newer = StudentDiscount.builder()
                .studentDiscountId(2L).student(student).discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN).validFrom(LocalDate.of(2026, 6, 1)).validUntil(null).active(true).build();

        when(studentDiscountRepository.findByStudentStudentIdAndActiveTrue(1L)).thenReturn(List.of(older, newer));

        Optional<StudentDiscount> effective = studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1));

        assertThat(effective).isPresent();
        assertThat(effective.get().getStudentDiscountId()).isEqualTo(2L);
    }

    @Test
    void findEffectiveDiscountIgnoresExpiredOrNotYetStarted() {
        Student student = Student.builder().studentId(1L).build();
        StudentDiscount expired = StudentDiscount.builder()
                .studentDiscountId(1L).student(student).discountType(DiscountType.FIXED_AMOUNT)
                .value(BigDecimal.TEN).validFrom(LocalDate.of(2026, 1, 1)).validUntil(LocalDate.of(2026, 3, 1)).active(true).build();
        StudentDiscount notYetStarted = StudentDiscount.builder()
                .studentDiscountId(2L).student(student).discountType(DiscountType.FIXED_AMOUNT)
                .value(BigDecimal.TEN).validFrom(LocalDate.of(2026, 12, 1)).validUntil(null).active(true).build();

        when(studentDiscountRepository.findByStudentStudentIdAndActiveTrue(1L)).thenReturn(List.of(expired, notYetStarted));

        Optional<StudentDiscount> effective = studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1));

        assertThat(effective).isEmpty();
    }
}
