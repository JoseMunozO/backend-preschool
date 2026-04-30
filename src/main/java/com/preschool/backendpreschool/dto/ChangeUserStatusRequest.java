package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
        @NotNull
        UserStatus status
) {
}
