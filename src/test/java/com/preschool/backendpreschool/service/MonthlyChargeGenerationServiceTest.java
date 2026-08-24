package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentDiscount;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.ChargeTypeRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyChargeGenerationServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ChargeTypeRepository chargeTypeRepository;

    @Mock
    private StudentChargeRepository studentChargeRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private StudentDiscountService studentDiscountService;

    @Spy
    private ChargeAmountCalculator chargeAmountCalculator = new ChargeAmountCalculator();

    @InjectMocks
    private MonthlyChargeGenerationService monthlyChargeGenerationService;

    @Test
    void returnsEmptyWhenNoMonthlyChargeTypesExist() {
        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(buildChargeType(ChargeRecurrenceType.ONE_TIME)));

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).isEmpty();
    }

    @Test
    void generatesFullAmountChargeForAlreadyEnrolledActiveStudent() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 1, 15), StudentStatus.active);

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                1L, 1L, LocalDate.of(2026, 8, 1))).thenReturn(false);
        when(studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1))).thenReturn(Optional.empty());
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> {
            StudentCharge charge = invocation.getArgument(0);
            charge.setStudentChargeId(100L);
            return charge;
        });
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(100L)).thenReturn(BigDecimal.ZERO);

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.amountDue()).isEqualByComparingTo("950.00");
            assertThat(response.billingPeriodStart()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(response.billingPeriodEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
        });
    }

    @Test
    void skipsWhenChargeAlreadyExistsForThatMonth() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 1, 15), StudentStatus.active);

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                1L, 1L, LocalDate.of(2026, 8, 1))).thenReturn(true);

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).isEmpty();
    }

    @Test
    void skipsStudentNotYetEnrolledThisMonth() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 9, 1), StudentStatus.active);

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).isEmpty();
    }

    @Test
    void skipsInactiveStudent() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 1, 1), StudentStatus.inactive);

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).isEmpty();
    }

    @Test
    void proratesAmountForMidMonthEnrollment() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        // Enrolled Aug 16: billable days = 31 - 16 + 1 = 16 out of 31
        Student student = buildStudent(LocalDate.of(2026, 8, 16), StudentStatus.active);

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                1L, 1L, LocalDate.of(2026, 8, 1))).thenReturn(false);
        when(studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1))).thenReturn(Optional.empty());
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> {
            StudentCharge charge = invocation.getArgument(0);
            charge.setStudentChargeId(100L);
            return charge;
        });
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(100L)).thenReturn(BigDecimal.ZERO);

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        BigDecimal expected = new BigDecimal("950.00")
                .multiply(BigDecimal.valueOf(16))
                .divide(BigDecimal.valueOf(31), 2, java.math.RoundingMode.HALF_UP);
        assertThat(result).singleElement()
                .satisfies(response -> assertThat(response.amountDue()).isEqualByComparingTo(expected));
    }

    @Test
    void appliesPercentageDiscountToBaseAmount() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 1, 1), StudentStatus.active);
        StudentDiscount discount = StudentDiscount.builder()
                .studentDiscountId(1L)
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .reason("Hermanos")
                .validFrom(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                1L, 1L, LocalDate.of(2026, 8, 1))).thenReturn(false);
        when(studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1))).thenReturn(Optional.of(discount));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> {
            StudentCharge charge = invocation.getArgument(0);
            charge.setStudentChargeId(100L);
            return charge;
        });
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(100L)).thenReturn(BigDecimal.ZERO);

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.amountDue()).isEqualByComparingTo("855.00");
            assertThat(response.description()).contains("Hermanos");
        });
    }

    @Test
    void appliesFixedAmountDiscountFlooredAtZero() {
        ChargeType chargeType = buildChargeType(ChargeRecurrenceType.MONTHLY);
        Student student = buildStudent(LocalDate.of(2026, 1, 1), StudentStatus.active);
        StudentDiscount discount = StudentDiscount.builder()
                .studentDiscountId(1L)
                .discountType(DiscountType.FIXED_AMOUNT)
                .value(new BigDecimal("2000"))
                .reason("Beca completa")
                .validFrom(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();

        when(chargeTypeRepository.findByActiveTrue()).thenReturn(List.of(chargeType));
        when(studentRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(student));
        when(studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                1L, 1L, LocalDate.of(2026, 8, 1))).thenReturn(false);
        when(studentDiscountService.findEffectiveDiscount(1L, LocalDate.of(2026, 8, 1))).thenReturn(Optional.of(discount));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> {
            StudentCharge charge = invocation.getArgument(0);
            charge.setStudentChargeId(100L);
            return charge;
        });
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(100L)).thenReturn(BigDecimal.ZERO);

        List<StudentChargeResponse> result = monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.of(2026, 8));

        assertThat(result).singleElement()
                .satisfies(response -> assertThat(response.amountDue()).isEqualByComparingTo("0.00"));
    }

    private ChargeType buildChargeType(ChargeRecurrenceType recurrenceType) {
        return ChargeType.builder()
                .chargeTypeId(1L)
                .code("MONTHLY_FEE")
                .name("Monthly fee")
                .recurrenceType(recurrenceType)
                .defaultAmount(new BigDecimal("950.00"))
                .active(true)
                .build();
    }

    private Student buildStudent(LocalDate enrollmentDate, StudentStatus status) {
        return Student.builder()
                .studentId(1L)
                .firstName("Ana")
                .lastName("Diaz")
                .enrollmentDate(enrollmentDate)
                .status(status)
                .build();
    }
}
