package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long userId,
        String email,
        String phone,
        UserStatus status,
        Boolean emailVerified,
        Boolean phoneVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<RoleResponse> roles
) {
}
