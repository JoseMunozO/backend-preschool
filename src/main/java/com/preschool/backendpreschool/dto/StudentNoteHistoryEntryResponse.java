package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentNoteType;

import java.time.LocalDateTime;
import java.util.List;

public record StudentNoteHistoryEntryResponse(
        Long studentNoteId,
        Long studentId,
        String studentName,
        Long authorUserId,
        String authorEmail,
        StudentNoteType noteType,
        String content,
        Boolean moderated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<StudentNoteAuditLogResponse> auditLog
) {
}
