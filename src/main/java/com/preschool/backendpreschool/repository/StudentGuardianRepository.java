package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentGuardian;
import com.preschool.backendpreschool.model.StudentGuardianId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, StudentGuardianId> {

    List<StudentGuardian> findByParentParentId(Long parentId);

    List<StudentGuardian> findByStudentStudentId(Long studentId);

    List<StudentGuardian> findByStudentStudentIdIn(List<Long> studentIds);
}
