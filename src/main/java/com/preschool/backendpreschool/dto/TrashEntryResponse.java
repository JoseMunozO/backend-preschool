package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.TrashEntityType;

import java.time.LocalDateTime;

public record TrashEntryResponse(
        Long entityId,
        TrashEntityType entityType,
        String label,
        LocalDateTime deletedAt,
        LocalDateTime purgeDeadline
) {
}
