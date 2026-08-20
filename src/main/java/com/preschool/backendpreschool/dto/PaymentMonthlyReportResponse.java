package com.preschool.backendpreschool.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PaymentMonthlyReportResponse(
        YearMonth month,
        long pendingCount,
        BigDecimal pendingBalance,
        List<StudentChargeResponse> pendingCharges,
        long overdueCount,
        BigDecimal overdueBalance,
        List<StudentChargeResponse> overdueCharges,
        BigDecimal paymentsReceived
) {
}
