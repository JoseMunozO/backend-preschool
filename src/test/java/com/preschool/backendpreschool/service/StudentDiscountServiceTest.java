package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentDiscountRequest;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountDurationType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.model.StudentDiscount;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentDiscountRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDiscountServiceTest {

    @Mock
    private StudentDiscountRepository studentDiscountRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentChargeRepository studentChargeRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Spy
    private ChargeAmountCalculator chargeAmountCalculator = new ChargeAmountCalculator();

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
        when(studentChargeRepository.findByStudentStudentIdAndStatusIn(anyLong(), any())).thenReturn(List.of());

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("10"), "Hermanos", LocalDate.of(2026, 8, 1), null
        );

        StudentDiscountResponse response = studentDiscountService.createDiscount(1L, request, "admin@school.com");

        assertThat(response.studentDiscountId()).isEqualTo(5L);
        assertThat(response.active()).isTrue();
        assertThat(response.reason()).isEqualTo("Hermanos");
    }

    @Test
    void createDiscountRecalculatesOpenChargeForCurrentBillingPeriod() {
        Student student = Student.builder()
                .studentId(1L).firstName("Ana").lastName("Diaz")
                .enrollmentDate(LocalDate.of(2019, 1, 1))
                .build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();
        ChargeType chargeType = ChargeType.builder()
                .chargeTypeId(10L).code("MONTHLY_FEE").name("Mensualidad")
                .recurrenceType(ChargeRecurrenceType.MONTHLY)
                .defaultAmount(new BigDecimal("1000.00"))
                .active(true)
                .build();
        // Billing period starts well before the discount's validFrom, mirroring the real bug: a
        // discount created mid-period (validFrom = today) must still apply to a charge whose
        // billingPeriodStart is the 1st of an already-in-progress period.
        StudentCharge openCharge = StudentCharge.builder()
                .studentChargeId(7L)
                .student(student)
                .chargeType(chargeType)
                .dueDate(LocalDate.of(2099, 1, 31))
                .billingPeriodStart(LocalDate.of(2020, 1, 1))
                .billingPeriodEnd(LocalDate.of(2020, 1, 31))
                .amountDue(new BigDecimal("1000.00"))
                .status(StudentChargeStatus.PENDING)
                .build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentDiscountRepository.save(any(StudentDiscount.class))).thenAnswer(invocation -> {
            StudentDiscount discount = invocation.getArgument(0);
            discount.setStudentDiscountId(5L);
            return discount;
        });
        when(studentChargeRepository.findByStudentStudentIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(openCharge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(7L)).thenReturn(BigDecimal.ZERO);
        when(studentDiscountRepository.findByStudentStudentIdAndActiveTrue(1L)).thenAnswer(invocation -> List.of(
                StudentDiscount.builder()
                        .studentDiscountId(5L).student(student).discountType(DiscountType.PERCENTAGE)
                        .value(new BigDecimal("10")).validFrom(LocalDate.of(2020, 6, 15)).active(true).build()
        ));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("10"), "Hermanos", LocalDate.of(2020, 6, 15), null
        );

        studentDiscountService.createDiscount(1L, request, "admin@school.com");

        assertThat(openCharge.getAmountDue()).isEqualByComparingTo("900.00");
        assertThat(openCharge.getStatus()).isEqualTo(StudentChargeStatus.PENDING);
    }

    @Test
    void createDiscountWithInstantDurationCoversOnlyCurrentBillingCycle() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentDiscountRepository.save(any(StudentDiscount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentChargeRepository.findByStudentStudentIdAndStatusIn(anyLong(), any())).thenReturn(List.of());

        // Client-supplied dates are irrelevant for an INSTANT discount - the server always
        // computes today..end-of-month regardless of what (if anything) is sent.
        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.INSTANT, new BigDecimal("10"), "Ajuste puntual",
                LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1)
        );

        StudentDiscountResponse response = studentDiscountService.createDiscount(1L, request, "admin@school.com");

        assertThat(response.durationType()).isEqualTo(DiscountDurationType.INSTANT);
        assertThat(response.validFrom()).isEqualTo(LocalDate.now());
        assertThat(response.validUntil()).isEqualTo(java.time.YearMonth.now().atEndOfMonth());
    }

    @Test
    void createDiscountRejectsScheduledWithoutValidFrom() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("10"), "Beca", null, null
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(1L, request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDiscountCapsValidUntilAtStudentWithdrawalDate() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz")
                .withdrawalDate(LocalDate.of(2026, 12, 31)).build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(studentDiscountRepository.save(any(StudentDiscount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentChargeRepository.findByStudentStudentIdAndStatusIn(anyLong(), any())).thenReturn(List.of());

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("10"), "Beca",
                LocalDate.of(2026, 1, 1), null
        );

        StudentDiscountResponse response = studentDiscountService.createDiscount(1L, request, "admin@school.com");

        assertThat(response.validUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void createDiscountRejectsValidFromAfterStudentWithdrawalDate() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz")
                .withdrawalDate(LocalDate.of(2026, 1, 1)).build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("10"), "Beca",
                LocalDate.of(2026, 6, 1), null
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(1L, request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDiscountRejectsPercentageAboveOneHundred() {
        Student student = Student.builder().studentId(1L).firstName("Ana").lastName("Diaz").build();
        User admin = User.builder().userId(2L).email("admin@school.com").build();

        when(studentRepository.findByStudentIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.PERCENTAGE, DiscountDurationType.SCHEDULED, new BigDecimal("150"), "Beca", LocalDate.of(2026, 8, 1), null
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
                DiscountType.FIXED_AMOUNT, DiscountDurationType.SCHEDULED, new BigDecimal("50"), "Referido", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)
        );

        assertThatThrownBy(() -> studentDiscountService.createDiscount(1L, request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDiscountForMissingStudentThrowsNotFound() {
        when(studentRepository.findByStudentIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        StudentDiscountRequest request = new StudentDiscountRequest(
                DiscountType.FIXED_AMOUNT, DiscountDurationType.SCHEDULED, new BigDecimal("50"), "Referido", LocalDate.of(2026, 8, 1), null
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
        when(studentChargeRepository.findByStudentStudentIdAndStatusIn(anyLong(), any())).thenReturn(List.of());

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

    @Test
    void findEffectiveDiscountStopsApplyingAfterStudentWithdrawalDateEvenIfStoredValidUntilIsLater() {
        Student student = Student.builder().studentId(1L).withdrawalDate(LocalDate.of(2026, 6, 30)).build();
        StudentDiscount discount = StudentDiscount.builder()
                .studentDiscountId(1L).student(student).discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN).validFrom(LocalDate.of(2026, 1, 1)).validUntil(LocalDate.of(2030, 1, 1)).active(true).build();

        when(studentDiscountRepository.findByStudentStudentIdAndActiveTrue(1L)).thenReturn(List.of(discount));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        Optional<StudentDiscount> beforeWithdrawal = studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 3, 1));
        Optional<StudentDiscount> afterWithdrawal = studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1));

        assertThat(beforeWithdrawal).isPresent();
        assertThat(afterWithdrawal).isEmpty();
    }
}
