package com.preschool.backendpreschool.dto;

import java.math.BigDecimal;

public record DashboardFinanceSummaryResponse(
        BigDecimal pendingBalance,
        BigDecimal overdueBalance,
        BigDecimal monthPaymentsReceived
) {
}
