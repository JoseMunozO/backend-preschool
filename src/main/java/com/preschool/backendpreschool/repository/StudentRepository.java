package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    long countByStatus(StudentStatus status);

    List<Student> findAllByDeletedAtIsNull();

    Optional<Student> findByStudentIdAndDeletedAtIsNull(Long studentId);

    Optional<Student> findByStudentIdAndDeletedAtIsNotNull(Long studentId);

    List<Student> findAllByDeletedAtIsNotNullAndDeletedAtBefore(LocalDateTime cutoff);

    List<Student> findAllByClassGroupGroupIdInAndDeletedAtIsNull(List<Long> groupIds);
}
