package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.ParentStatus;

import java.time.LocalDateTime;

public record ParentResponse(
        Long parentId,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String preferredLanguage,
        ParentStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
