package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByPaymentPaymentId(Long paymentId);

    List<PaymentAllocation> findByStudentChargeStudentChargeId(Long studentChargeId);

    @Query("select coalesce(sum(a.amountAllocated), 0) from PaymentAllocation a where a.studentCharge.studentChargeId = :studentChargeId")
    BigDecimal sumAllocatedByStudentChargeId(@Param("studentChargeId") Long studentChargeId);
}
