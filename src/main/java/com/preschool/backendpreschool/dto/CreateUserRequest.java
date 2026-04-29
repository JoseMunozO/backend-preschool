package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Email
        String email,

        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotEmpty
        Set<RoleName> roles
) {
}
