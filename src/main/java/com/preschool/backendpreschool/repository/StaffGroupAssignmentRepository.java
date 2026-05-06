package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StaffGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffGroupAssignmentRepository extends JpaRepository<StaffGroupAssignment, Long> {

    List<StaffGroupAssignment> findByClassGroupGroupIdOrderByStartDateDesc(Long groupId);

    List<StaffGroupAssignment> findByStaffStaffIdOrderByStartDateDesc(Long staffId);
}
