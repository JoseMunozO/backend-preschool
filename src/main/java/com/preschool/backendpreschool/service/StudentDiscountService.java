package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentDiscountRequest;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentDiscountService {

    private final StudentDiscountRepository studentDiscountRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentChargeRepository studentChargeRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final ChargeAmountCalculator chargeAmountCalculator;

    public List<StudentDiscountResponse> getDiscounts(Long studentId) {
        findStudent(studentId);
        return studentDiscountRepository.findByStudentStudentIdOrderByValidFromDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentDiscountResponse createDiscount(Long studentId, StudentDiscountRequest request, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);

        if (request.discountType() == DiscountType.PERCENTAGE && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje de descuento no puede ser mayor a 100");
        }
        if (request.validUntil() != null && request.validUntil().isBefore(request.validFrom())) {
            throw new BadRequestException("La fecha 'validUntil' no puede ser anterior a 'validFrom'");
        }

        StudentDiscount discount = StudentDiscount.builder()
                .student(student)
                .discountType(request.discountType())
                .value(request.value())
                .reason(trimToNull(request.reason()))
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .active(true)
                .createdByUser(requester)
                .build();

        StudentDiscountResponse response = toResponse(studentDiscountRepository.save(discount));
        recalculateOpenCharges(studentId);
        return response;
    }

    @Transactional
    public StudentDiscountResponse deactivateDiscount(Long studentId, Long discountId) {
        StudentDiscount discount = studentDiscountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado"));

        if (!discount.getStudent().getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Descuento no encontrado");
        }

        discount.setActive(false);
        StudentDiscountResponse response = toResponse(studentDiscountRepository.save(discount));
        recalculateOpenCharges(studentId);
        return response;
    }

    /**
     * Charge generation only prices in the discount active at the moment a charge is created,
     * and never re-runs for a period that already has a charge. Without this, creating or
     * deactivating a discount would only take effect starting next month's charge instead of
     * updating what the family currently owes.
     */
    private void recalculateOpenCharges(Long studentId) {
        List<StudentCharge> openCharges = studentChargeRepository.findByStudentStudentIdAndStatusIn(
                studentId,
                List.of(StudentChargeStatus.PENDING, StudentChargeStatus.PARTIALLY_PAID, StudentChargeStatus.OVERDUE)
        );

        for (StudentCharge charge : openCharges) {
            recalculateCharge(charge);
        }
    }

    private void recalculateCharge(StudentCharge charge) {
        ChargeType chargeType = charge.getChargeType();
        if (chargeType.getRecurrenceType() != ChargeRecurrenceType.MONTHLY
                || chargeType.getDefaultAmount() == null
                || charge.getBillingPeriodStart() == null
                || charge.getBillingPeriodEnd() == null) {
            return;
        }

        Student student = charge.getStudent();
        BigDecimal baseAmount = chargeAmountCalculator.computeBaseAmount(
                chargeType, student, charge.getBillingPeriodStart(), charge.getBillingPeriodEnd());
        // Unlike generation (which prices a charge as of its own billing-period start), recalculation
        // reprices an already-open charge for a discount that may have been created mid-period - so the
        // discount lookup must use today, not the charge's billingPeriodStart, or a discount created after
        // the 1st of the month would never look "effective" for that period and would be silently ignored.
        Optional<StudentDiscount> discount = findEffectiveDiscount(student.getStudentId(), LocalDate.now());
        BigDecimal finalAmount = discount.map(d -> chargeAmountCalculator.applyDiscount(baseAmount, d)).orElse(baseAmount);

        charge.setAmountDue(finalAmount);
        charge.setDescription(chargeAmountCalculator.buildDescription(chargeType, charge.getBillingPeriodStart(), discount.orElse(null)));

        BigDecimal paid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            charge.setStatus(charge.getDueDate().isBefore(LocalDate.now()) ? StudentChargeStatus.OVERDUE : StudentChargeStatus.PENDING);
        } else if (paid.compareTo(finalAmount) >= 0) {
            charge.setStatus(StudentChargeStatus.PAID);
        } else {
            charge.setStatus(StudentChargeStatus.PARTIALLY_PAID);
        }

        studentChargeRepository.save(charge);
    }

    /**
     * Currently-valid discount for a student on a given date, used when generating charges.
     * If more than one happens to be valid at once, the most recently started one wins.
     */
    public Optional<StudentDiscount> findEffectiveDiscount(Long studentId, LocalDate date) {
        return studentDiscountRepository.findByStudentStudentIdAndActiveTrue(studentId)
                .stream()
                .filter(discount -> !discount.getValidFrom().isAfter(date)
                        && (discount.getValidUntil() == null || !discount.getValidUntil().isBefore(date)))
                .max(Comparator.comparing(StudentDiscount::getValidFrom));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findByStudentIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private StudentDiscountResponse toResponse(StudentDiscount discount) {
        Student student = discount.getStudent();
        User createdBy = discount.getCreatedByUser();

        return new StudentDiscountResponse(
                discount.getStudentDiscountId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                discount.getDiscountType(),
                discount.getValue(),
                discount.getReason(),
                discount.getValidFrom(),
                discount.getValidUntil(),
                discount.getActive(),
                createdBy != null ? createdBy.getUserId() : null,
                createdBy != null ? createdBy.getEmail() : null,
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
