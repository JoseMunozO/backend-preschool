package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    long countByStatus(StudentStatus status);
}
