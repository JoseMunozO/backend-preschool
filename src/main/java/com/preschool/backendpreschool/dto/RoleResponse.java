package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.RoleName;

public record RoleResponse(
        Long roleId,
        RoleName code,
        String name
) {
}
