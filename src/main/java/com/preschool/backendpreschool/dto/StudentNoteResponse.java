package com.preschool.backendpreschool.dto;

import com.preschool.backendpreschool.model.StudentNoteType;

import java.time.LocalDateTime;

public record StudentNoteResponse(
        Long studentNoteId,
        Long studentId,
        String studentName,
        Long authorUserId,
        String authorEmail,
        StudentNoteType noteType,
        String content,
        Boolean moderated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
