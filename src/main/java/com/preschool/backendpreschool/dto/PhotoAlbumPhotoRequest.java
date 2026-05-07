package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhotoAlbumPhotoRequest(
        Long studentId,

        @NotBlank
        @Size(max = 500)
        String photoUrl,

        @Size(max = 500)
        String caption
) {
}
