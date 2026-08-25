package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pricing logic shared between monthly charge generation and applying a discount to one charge.
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

    public BigDecimal applyDiscount(BigDecimal baseAmount, DiscountType discountType, BigDecimal discountValue) {
        BigDecimal reduced;
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal factor = BigDecimal.ONE.subtract(
                    discountValue.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            reduced = baseAmount.multiply(factor);
        } else {
            reduced = baseAmount.subtract(discountValue);
        }

        return reduced.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public String buildDescription(ChargeType chargeType, LocalDate periodStart) {
        return chargeType.getName() + " - " + periodStart.getYear() + "-" + String.format("%02d", periodStart.getMonthValue());
    }
}
