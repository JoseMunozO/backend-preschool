package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.RoleName;

import java.time.LocalDateTime;

public record RoleResponse(
        Long roleId,
        RoleName code,
        String name,
        String description,
        LocalDateTime createdAt
) {
}
