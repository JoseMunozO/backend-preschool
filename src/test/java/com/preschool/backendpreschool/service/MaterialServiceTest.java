package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.MaterialMovementRequest;
import com.preschool.backendpreschool.dto.MaterialMovementResponse;
import com.preschool.backendpreschool.dto.MaterialResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Material;
import com.preschool.backendpreschool.model.MaterialMovement;
import com.preschool.backendpreschool.model.MaterialMovementType;
import com.preschool.backendpreschool.model.MaterialStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.MaterialMovementRepository;
import com.preschool.backendpreschool.repository.MaterialRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialMovementRepository materialMovementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void registerInMovementIncreasesStockAndStoresResponsibleUser() {
        Material material = buildMaterial(8, 5);
        User user = User.builder()
                .userId(4L)
                .email("admin@school.com")
                .build();

        when(materialRepository.findByMaterialIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(material));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(user));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialMovementRepository.save(any(MaterialMovement.class))).thenAnswer(invocation -> {
            MaterialMovement movement = invocation.getArgument(0);
            movement.setMaterialMovementId(20L);
            return movement;
        });

        MaterialMovementResponse response = materialService.registerMovement(
                1L,
                new MaterialMovementRequest(MaterialMovementType.IN, 4, "Compra de reposicion"),
                "admin@school.com"
        );

        assertThat(material.getQuantityOnHand()).isEqualTo(12);
        assertThat(response.materialMovementId()).isEqualTo(20L);
        assertThat(response.movementType()).isEqualTo(MaterialMovementType.IN);
        assertThat(response.performedByUserId()).isEqualTo(4L);
    }

    @Test
    void registerOutMovementRejectsWhenQuantityWouldBeNegative() {
        Material material = buildMaterial(3, 5);

        when(materialRepository.findByMaterialIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(material));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.registerMovement(
                1L,
                new MaterialMovementRequest(MaterialMovementType.OUT, 4, "Salida no valida"),
                "admin@school.com"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La salida supera la cantidad disponible");

        verify(materialRepository, never()).save(any(Material.class));
        verify(materialMovementRepository, never()).save(any(MaterialMovement.class));
    }

    @Test
    void registerAdjustmentMovementSetsStockToCountedQuantity() {
        Material material = buildMaterial(8, 5);

        when(materialRepository.findByMaterialIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialMovementRepository.save(any(MaterialMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        materialService.registerMovement(
                1L,
                new MaterialMovementRequest(MaterialMovementType.ADJUSTMENT, 6, "Conteo fisico"),
                null
        );

        assertThat(material.getQuantityOnHand()).isEqualTo(6);
    }

    @Test
    void deleteMaterialSoftDeletesInsteadOfRemovingRow() {
        Material material = buildMaterial(8, 5);

        when(materialRepository.findByMaterialIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        materialService.deleteMaterial(1L);

        assertThat(material.getDeletedAt()).isNotNull();
        verify(materialRepository, never()).delete(any(Material.class));
        verify(materialRepository).save(material);
    }

    @Test
    void restoreMaterialWithinGraceWindowClearsDeletedAt() {
        Material material = buildMaterial(8, 5);
        material.setDeletedAt(LocalDateTime.now().minusDays(3));

        when(materialRepository.findByMaterialIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialResponse response = materialService.restoreMaterial(1L);

        assertThat(material.getDeletedAt()).isNull();
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void restoreMaterialAfterGraceWindowThrowsConflict() {
        Material material = buildMaterial(8, 5);
        material.setDeletedAt(LocalDateTime.now().minusDays(8));

        when(materialRepository.findByMaterialIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> materialService.restoreMaterial(1L))
                .isInstanceOf(ConflictException.class);

        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void restoreMaterialNotDeletedThrowsNotFound() {
        when(materialRepository.findByMaterialIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.restoreMaterial(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMaterialsWithIncludeDeletedTrueReturnsSoftDeletedMaterials() {
        Material deleted = buildMaterial(8, 5);
        deleted.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(materialRepository.findAll()).thenReturn(List.of(deleted));

        List<MaterialResponse> response = materialService.getMaterials(null, null, null, null, true);

        assertThat(response).extracting(MaterialResponse::materialId).containsExactly(1L);
        verify(materialRepository, never()).findAllByDeletedAtIsNull();
    }

    @Test
    void purgeExpiredSoftDeletedMaterialsDeletesExpiredAndSkipsRestrictedOnes() {
        Material purgeable = buildMaterial(8, 5);
        Material restricted = Material.builder()
                .materialId(2L)
                .sku("MAT-002")
                .name("Papel de dibujo")
                .quantityOnHand(3)
                .minimumQuantity(1)
                .status(MaterialStatus.ACTIVE)
                .build();

        when(materialRepository.findAllByDeletedAtIsNotNullAndDeletedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(purgeable, restricted));
        doNothing().when(materialRepository).delete(purgeable);
        doThrow(new DataIntegrityViolationException("has related movements"))
                .when(materialRepository).delete(restricted);

        materialService.purgeExpiredSoftDeletedMaterials();

        verify(materialRepository).delete(purgeable);
        verify(materialRepository).delete(restricted);
    }

    private Material buildMaterial(Integer quantityOnHand, Integer minimumQuantity) {
        return Material.builder()
                .materialId(1L)
                .sku("MAT-001")
                .name("Toallas de papel")
                .category("limpieza")
                .unit("paquete")
                .quantityOnHand(quantityOnHand)
                .minimumQuantity(minimumQuantity)
                .status(MaterialStatus.ACTIVE)
                .notes("Material de prueba")
                .build();
    }
}
