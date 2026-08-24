package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChargeTypeRepository extends JpaRepository<ChargeType, Long> {

    Optional<ChargeType> findByCode(String code);

    List<ChargeType> findByActiveTrue();

    boolean existsByCode(String code);
}
