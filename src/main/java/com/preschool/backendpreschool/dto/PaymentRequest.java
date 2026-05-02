package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentRequest(
        Long parentId,
        Long receivedByStaffId,

        @NotNull
        LocalDate paymentDate,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal totalAmount,

        @NotNull
        PaymentMethod paymentMethod,

        @Size(max = 100)
        String referenceNumber,

        String notes,

        @NotEmpty
        List<@Valid PaymentAllocationRequest> allocations
) {
}
