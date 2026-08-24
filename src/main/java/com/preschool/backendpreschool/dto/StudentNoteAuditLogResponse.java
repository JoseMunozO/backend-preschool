package com.preschool.backendpreschool.dto;

import java.time.LocalDateTime;

public record StudentNoteAuditLogResponse(
        Long studentNoteAuditLogId,
        Long studentNoteId,
        Long changedByUserId,
        String changedByEmail,
        LocalDateTime changedAt,
        String previousValues,
        String newValues
) {
}
