package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentProfilePhotoRequest(
        @NotBlank
        @Size(max = 500)
        String profilePhotoUrl
) {
}
