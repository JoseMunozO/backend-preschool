package com.preschool.backendpreschool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record StaffResponse(
        Long staffId,
        Long userId,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        String positionTitle,
        String staffType,
        LocalDate hireDate,
        LocalDate accessExpiresAt,
        String status,
        String notes,
        Set<RoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
