package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.RoleName;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull
        RoleName role
) {
}
