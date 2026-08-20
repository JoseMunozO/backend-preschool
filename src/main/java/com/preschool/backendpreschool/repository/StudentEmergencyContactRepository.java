package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEmergencyContactRepository extends JpaRepository<StudentEmergencyContact, Long> {

    List<StudentEmergencyContact> findByStudentStudentIdOrderByPrimaryDescFullNameAsc(Long studentId);
}
