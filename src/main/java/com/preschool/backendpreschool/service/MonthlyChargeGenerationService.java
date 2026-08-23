package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.model.StudentDiscount;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.ChargeTypeRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonthlyChargeGenerationService {

    private final StudentRepository studentRepository;
    private final ChargeTypeRepository chargeTypeRepository;
    private final StudentChargeRepository studentChargeRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final StudentDiscountService studentDiscountService;

    @Transactional
    public List<StudentChargeResponse> generateMonthlyCharges(YearMonth month) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();

        List<ChargeType> monthlyChargeTypes = chargeTypeRepository.findByActiveTrue()
                .stream()
                .filter(chargeType -> chargeType.getRecurrenceType() == ChargeRecurrenceType.MONTHLY)
                .filter(chargeType -> chargeType.getDefaultAmount() != null)
                .toList();

        if (monthlyChargeTypes.isEmpty()) {
            return List.of();
        }

        List<Student> eligibleStudents = studentRepository.findAllByDeletedAtIsNull()
                .stream()
                .filter(student -> student.getStatus() == StudentStatus.active)
                .filter(student -> student.getEnrollmentDate() != null && !student.getEnrollmentDate().isAfter(periodEnd))
                .toList();

        List<StudentChargeResponse> created = new ArrayList<>();
        for (Student student : eligibleStudents) {
            for (ChargeType chargeType : monthlyChargeTypes) {
                if (studentChargeRepository.existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
                        student.getStudentId(), chargeType.getChargeTypeId(), periodStart)) {
                    continue;
                }

                created.add(toResponse(generateCharge(student, chargeType, periodStart, periodEnd)));
            }
        }

        return created;
    }

    private StudentCharge generateCharge(Student student, ChargeType chargeType, LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal baseAmount = computeBaseAmount(chargeType, student, periodStart, periodEnd);
        Optional<StudentDiscount> discount = studentDiscountService.findEffectiveDiscount(student.getStudentId(), periodStart);
        BigDecimal finalAmount = discount.map(d -> applyDiscount(baseAmount, d)).orElse(baseAmount);

        StudentCharge charge = StudentCharge.builder()
                .student(student)
                .chargeType(chargeType)
                .dueDate(periodEnd)
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .amountDue(finalAmount)
                .status(StudentChargeStatus.PENDING)
                .description(buildDescription(chargeType, periodStart, discount.orElse(null)))
                .build();

        return studentChargeRepository.save(charge);
    }

    private BigDecimal computeBaseAmount(ChargeType chargeType, Student student, LocalDate periodStart, LocalDate periodEnd) {
        BigDecimal defaultAmount = chargeType.getDefaultAmount();
        LocalDate enrollmentDate = student.getEnrollmentDate();

        if (enrollmentDate.isAfter(periodStart)) {
            int totalDays = periodEnd.getDayOfMonth();
            int billableDays = totalDays - enrollmentDate.getDayOfMonth() + 1;
            BigDecimal ratio = BigDecimal.valueOf(billableDays)
                    .divide(BigDecimal.valueOf(totalDays), 6, RoundingMode.HALF_UP);
            return defaultAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        }

        return defaultAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyDiscount(BigDecimal baseAmount, StudentDiscount discount) {
        BigDecimal reduced;
        if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal factor = BigDecimal.ONE.subtract(
                    discount.getValue().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            reduced = baseAmount.multiply(factor);
        } else {
            reduced = baseAmount.subtract(discount.getValue());
        }

        return reduced.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildDescription(ChargeType chargeType, LocalDate periodStart, StudentDiscount discount) {
        String base = chargeType.getName() + " - " + periodStart.getYear() + "-" + String.format("%02d", periodStart.getMonthValue());
        if (discount == null) {
            return base;
        }

        String discountLabel = discount.getDiscountType() == DiscountType.PERCENTAGE
                ? discount.getValue() + "%"
                : discount.getValue().toString();
        String reason = discount.getReason() != null ? discount.getReason() : "descuento";
        return base + " (" + reason + ": -" + discountLabel + ")";
    }

    private StudentChargeResponse toResponse(StudentCharge charge) {
        BigDecimal paid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());
        BigDecimal balance = charge.getAmountDue().subtract(paid);
        Student student = charge.getStudent();
        ChargeType chargeType = charge.getChargeType();

        return new StudentChargeResponse(
                charge.getStudentChargeId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                chargeType.getChargeTypeId(),
                chargeType.getCode(),
                chargeType.getName(),
                charge.getDueDate(),
                charge.getBillingPeriodStart(),
                charge.getBillingPeriodEnd(),
                charge.getAmountDue(),
                paid,
                balance.max(BigDecimal.ZERO),
                charge.getStatus(),
                charge.getDescription(),
                charge.getCreatedAt(),
                charge.getUpdatedAt()
        );
    }
}
