package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentAttendance;
import com.preschool.backendpreschool.model.StudentAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    Optional<StudentAttendance> findByStudentStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    List<StudentAttendance> findByAttendanceDateAndStudentStudentIdIn(LocalDate attendanceDate, List<Long> studentIds);

    long countByAttendanceDateAndStatus(LocalDate attendanceDate, StudentAttendanceStatus status);

    List<StudentAttendance> findByStudentStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long studentId, LocalDate from, LocalDate to);

    List<StudentAttendance> findByAttendanceDateBetweenAndStudentStudentIdIn(
            LocalDate from, LocalDate to, List<Long> studentIds);
}
