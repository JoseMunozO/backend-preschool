package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.MaterialMovementRequest;
import com.preschool.backendpreschool.dto.MaterialMovementResponse;
import com.preschool.backendpreschool.dto.MaterialRequest;
import com.preschool.backendpreschool.dto.MaterialResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.MaterialMovement;
import com.preschool.backendpreschool.model.MaterialMovementType;
import com.preschool.backendpreschool.model.MaterialStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.MaterialMovementRepository;
import com.preschool.backendpreschool.repository.MaterialRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialMovementRepository materialMovementRepository;
    private final UserRepository userRepository;

    public List<MaterialResponse> getMaterials(String search, String category, MaterialStatus status, Boolean lowStock) {
        return materialRepository.findAll()
                .stream()
                .filter(material -> search == null || matchesSearch(material, search))
                .filter(material -> category == null || equalsIgnoreCase(material.getCategory(), category))
                .filter(material -> status == null || material.getStatus() == status)
                .filter(material -> !Boolean.TRUE.equals(lowStock) || isLowStock(material))
                .map(this::toMaterialResponse)
                .toList();
    }

    public List<MaterialResponse> getLowStockMaterials() {
        return materialRepository.findActiveLowStock()
                .stream()
                .map(this::toMaterialResponse)
                .toList();
    }

    public MaterialResponse getMaterialById(Long materialId) {
        return toMaterialResponse(findMaterial(materialId));
    }

    @Transactional
    public MaterialResponse createMaterial(MaterialRequest request) {
        String sku = trimToNull(request.sku());
        if (sku != null && materialRepository.existsBySku(sku)) {
            throw new BadRequestException("Ya existe un material con ese SKU");
        }

        Material material = Material.builder()
                .sku(sku)
                .name(request.name().trim())
                .category(trimToNull(request.category()))
                .unit(trimToNull(request.unit()))
                .quantityOnHand(request.quantityOnHand())
                .minimumQuantity(request.minimumQuantity())
                .status(request.status() != null ? request.status() : MaterialStatus.ACTIVE)
                .notes(trimToNull(request.notes()))
                .build();

        return toMaterialResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse updateMaterial(Long materialId, MaterialRequest request) {
        Material material = findMaterial(materialId);
        String sku = trimToNull(request.sku());
        if (sku != null && !sku.equals(material.getSku()) && materialRepository.existsBySku(sku)) {
            throw new BadRequestException("Ya existe un material con ese SKU");
        }

        material.setSku(sku);
        material.setName(request.name().trim());
        material.setCategory(trimToNull(request.category()));
        material.setUnit(trimToNull(request.unit()));
        material.setQuantityOnHand(request.quantityOnHand());
        material.setMinimumQuantity(request.minimumQuantity());
        material.setStatus(request.status() != null ? request.status() : MaterialStatus.ACTIVE);
        material.setNotes(trimToNull(request.notes()));

        return toMaterialResponse(materialRepository.save(material));
    }

    public List<MaterialMovementResponse> getMovements(Long materialId) {
        if (materialId != null && !materialRepository.existsById(materialId)) {
            throw new ResourceNotFoundException("Material no encontrado");
        }

        List<MaterialMovement> movements = materialId == null
                ? materialMovementRepository.findAllByOrderByCreatedAtDesc()
                : materialMovementRepository.findByMaterialMaterialIdOrderByCreatedAtDesc(materialId);

        return movements.stream()
                .map(this::toMovementResponse)
                .toList();
    }

    @Transactional
    public MaterialMovementResponse registerMovement(Long materialId, MaterialMovementRequest request, String performedByEmail) {
        Material material = findMaterial(materialId);
        User performedBy = performedByEmail == null ? null : userRepository.findByEmail(performedByEmail).orElse(null);
        Integer newQuantity = calculateNewQuantity(material, request);

        material.setQuantityOnHand(newQuantity);
        Material savedMaterial = materialRepository.save(material);

        MaterialMovement movement = MaterialMovement.builder()
                .material(savedMaterial)
                .movementType(request.movementType())
                .quantity(request.quantity())
                .performedByUser(performedBy)
                .notes(trimToNull(request.notes()))
                .build();

        return toMovementResponse(materialMovementRepository.save(movement));
    }

    private Integer calculateNewQuantity(Material material, MaterialMovementRequest request) {
        if (request.movementType() == MaterialMovementType.IN) {
            return material.getQuantityOnHand() + request.quantity();
        }

        if (request.movementType() == MaterialMovementType.OUT) {
            int newQuantity = material.getQuantityOnHand() - request.quantity();
            if (newQuantity < 0) {
                throw new BadRequestException("La salida supera la cantidad disponible");
            }
            return newQuantity;
        }

        return request.quantity();
    }

    private Material findMaterial(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));
    }

    private MaterialResponse toMaterialResponse(Material material) {
        return new MaterialResponse(
                material.getMaterialId(),
                material.getSku(),
                material.getName(),
                material.getCategory(),
                material.getUnit(),
                material.getQuantityOnHand(),
                material.getMinimumQuantity(),
                isLowStock(material),
                material.getStatus(),
                material.getNotes(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }

    private MaterialMovementResponse toMovementResponse(MaterialMovement movement) {
        Material material = movement.getMaterial();
        User performedBy = movement.getPerformedByUser();

        return new MaterialMovementResponse(
                movement.getMaterialMovementId(),
                material.getMaterialId(),
                material.getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getMovementDate(),
                performedBy != null ? performedBy.getUserId() : null,
                performedBy != null ? performedBy.getEmail() : null,
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }

    private boolean matchesSearch(Material material, String search) {
        String normalized = search.toLowerCase();
        return containsIgnoreCase(material.getName(), normalized)
                || containsIgnoreCase(material.getSku(), normalized)
                || containsIgnoreCase(material.getCategory(), normalized);
    }

    private boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase().contains(normalizedSearch);
    }

    private boolean equalsIgnoreCase(String value, String expected) {
        return value != null && value.equalsIgnoreCase(expected);
    }

    private boolean isLowStock(Material material) {
        return material.getQuantityOnHand() <= material.getMinimumQuantity();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
