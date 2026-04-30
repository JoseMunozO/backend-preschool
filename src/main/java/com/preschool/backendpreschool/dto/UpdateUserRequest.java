package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email
        @Size(max = 150)
        String email,

        @Size(max = 30)
        String phone
) {
}
