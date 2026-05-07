package com.preschool.backendpreschool.dto;

import java.time.LocalDateTime;

public record PhotoAlbumPhotoResponse(
        Long photoAlbumPhotoId,
        Long photoAlbumId,
        Long studentId,
        String studentName,
        Long uploadedByUserId,
        String uploadedByEmail,
        String photoUrl,
        String caption,
        Boolean approved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
