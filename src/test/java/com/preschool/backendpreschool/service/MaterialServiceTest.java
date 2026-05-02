package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.MaterialMovementRequest;
import com.preschool.backendpreschool.dto.MaterialMovementResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
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

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
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

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(materialMovementRepository.save(any(MaterialMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        materialService.registerMovement(
                1L,
                new MaterialMovementRequest(MaterialMovementType.ADJUSTMENT, 6, "Conteo fisico"),
                null
        );

        assertThat(material.getQuantityOnHand()).isEqualTo(6);
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
