package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {

    List<StudentNote> findByStudentStudentIdAndDeletedFalseOrderByCreatedAtDesc(Long studentId);
}
