package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentDiscount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pricing logic shared between initial monthly charge generation and later
 * recalculation of already-generated charges (e.g. when a discount changes).
 */
@Component
public class ChargeAmountCalculator {

    public BigDecimal computeBaseAmount(ChargeType chargeType, Student student, LocalDate periodStart, LocalDate periodEnd) {
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

    public BigDecimal applyDiscount(BigDecimal baseAmount, StudentDiscount discount) {
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

    public String buildDescription(ChargeType chargeType, LocalDate periodStart, StudentDiscount discount) {
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
}
