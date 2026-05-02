package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.MaterialMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialMovementRepository extends JpaRepository<MaterialMovement, Long> {

    List<MaterialMovement> findByMaterialMaterialIdOrderByCreatedAtDesc(Long materialId);

    List<MaterialMovement> findAllByOrderByCreatedAtDesc();
}
