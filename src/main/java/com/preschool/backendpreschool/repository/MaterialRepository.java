package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.MaterialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Material> findByStatus(MaterialStatus status);

    List<Material> findByCategoryIgnoreCase(String category);

    List<Material> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku);

    @Query("select m from Material m where m.status = com.preschool.backendpreschool.model.MaterialStatus.ACTIVE and m.quantityOnHand <= m.minimumQuantity")
    List<Material> findActiveLowStock();
}
