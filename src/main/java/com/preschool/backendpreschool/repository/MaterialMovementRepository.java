package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.MaterialMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MaterialMovementRepository extends JpaRepository<MaterialMovement, Long> {

    List<MaterialMovement> findByMaterialMaterialIdOrderByCreatedAtDesc(Long materialId);

    List<MaterialMovement> findAllByOrderByCreatedAtDesc();

    List<MaterialMovement> findByMaterialMaterialIdOrderByMovementDateAsc(Long materialId);

    List<MaterialMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(m.quantity), 0) from MaterialMovement m "
            + "where m.material.materialId = :materialId "
            + "and m.movementType = com.preschool.backendpreschool.model.MaterialMovementType.OUT "
            + "and m.movementDate >= :since")
    int sumOutQuantitySince(@Param("materialId") Long materialId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from MaterialMovement m where m.movementDate < :cutoff")
    int deleteAllByMovementDateBefore(@Param("cutoff") LocalDateTime cutoff);
}
