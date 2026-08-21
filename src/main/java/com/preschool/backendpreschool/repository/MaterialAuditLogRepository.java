package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.MaterialAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MaterialAuditLogRepository extends JpaRepository<MaterialAuditLog, Long> {

    List<MaterialAuditLog> findByMaterialMaterialIdOrderByChangedAtDesc(Long materialId);

    @Modifying
    @Query("delete from MaterialAuditLog a where a.changedAt < :cutoff")
    int deleteAllByChangedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
