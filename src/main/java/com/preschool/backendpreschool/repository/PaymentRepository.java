package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByParentParentId(Long parentId);

    List<Payment> findByPaymentDateBetween(LocalDate start, LocalDate end);
}
