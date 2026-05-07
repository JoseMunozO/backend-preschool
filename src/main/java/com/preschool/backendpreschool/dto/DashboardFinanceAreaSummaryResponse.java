package com.preschool.backendpreschool.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record DashboardFinanceAreaSummaryResponse(
        LocalDate date,
        YearMonth month,
        long pendingCharges,
        long overdueCharges,
        BigDecimal pendingBalance,
        BigDecimal overdueBalance,
        BigDecimal monthPaymentsReceived
) {
}
