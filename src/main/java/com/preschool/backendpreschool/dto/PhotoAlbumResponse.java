package com.preschool.backendpreschool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PhotoAlbumResponse(
        Long photoAlbumId,
        String title,
        String description,
        Long groupId,
        String groupName,
        Long studentId,
        String studentName,
        Long createdByUserId,
        String createdByEmail,
        LocalDate eventDate,
        Boolean active,
        List<PhotoAlbumPhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
