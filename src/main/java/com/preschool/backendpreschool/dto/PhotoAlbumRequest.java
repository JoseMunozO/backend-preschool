package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PhotoAlbumRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @Size(max = 5000)
        String description,

        Long groupId,
        Long studentId,
        LocalDate eventDate
) {
}
