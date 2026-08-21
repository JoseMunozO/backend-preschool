package com.preschool.backendpreschool.dto;

import java.time.LocalDateTime;

public record MaterialAuditLogResponse(
        Long materialAuditLogId,
        Long materialId,
        String materialName,
        Long changedByUserId,
        String changedByEmail,
        String changedByName,
        LocalDateTime changedAt,
        String previousValues,
        String newValues
) {
}
