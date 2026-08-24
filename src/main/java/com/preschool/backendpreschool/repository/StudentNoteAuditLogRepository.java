package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentNoteAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentNoteAuditLogRepository extends JpaRepository<StudentNoteAuditLog, Long> {

    List<StudentNoteAuditLog> findByStudentNoteStudentNoteIdOrderByChangedAtDesc(Long studentNoteId);
}
