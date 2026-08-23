package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudentChargeRepository extends JpaRepository<StudentCharge, Long> {

    List<StudentCharge> findByStudentStudentId(Long studentId);

    List<StudentCharge> findByStatus(StudentChargeStatus status);

    List<StudentCharge> findByBillingPeriodStartGreaterThanEqualAndBillingPeriodEndLessThanEqual(
            LocalDate start,
            LocalDate end
    );

    boolean existsByStudentStudentIdAndChargeTypeChargeTypeIdAndBillingPeriodStart(
            Long studentId,
            Long chargeTypeId,
            LocalDate billingPeriodStart
    );
}
