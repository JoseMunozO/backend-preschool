package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StaffGroupRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StaffGroupAssignmentResponse(
        Long staffGroupAssignmentId,
        Long staffId,
        String staffName,
        Long groupId,
        String groupName,
        StaffGroupRole roleInGroup,
        Boolean primary,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {
}
