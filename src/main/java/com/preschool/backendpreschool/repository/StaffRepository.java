package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUserEmail(String email);

    List<Staff> findAllByDeletedAtIsNull();

    List<Staff> findAllByDeletedAtIsNotNull();

    Optional<Staff> findByStaffIdAndDeletedAtIsNull(Long staffId);

    Optional<Staff> findByStaffIdAndDeletedAtIsNotNull(Long staffId);

    List<Staff> findAllByDeletedAtIsNullAndAccessExpiresAtIsNotNullAndAccessExpiresAtLessThanEqual(LocalDate date);
}
