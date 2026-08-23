package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDiscountRepository extends JpaRepository<StudentDiscount, Long> {

    List<StudentDiscount> findByStudentStudentIdOrderByValidFromDesc(Long studentId);

    List<StudentDiscount> findByStudentStudentIdAndActiveTrue(Long studentId);
}
