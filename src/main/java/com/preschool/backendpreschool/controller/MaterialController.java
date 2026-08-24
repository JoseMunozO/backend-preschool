package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.MaterialAuditLogResponse;
import com.preschool.backendpreschool.dto.MaterialMovementReportEntryResponse;
import com.preschool.backendpreschool.dto.MaterialMovementRequest;
import com.preschool.backendpreschool.dto.MaterialMovementResponse;
import com.preschool.backendpreschool.dto.MaterialRequest;
import com.preschool.backendpreschool.dto.MaterialResponse;
import com.preschool.backendpreschool.dto.MaterialSuggestedMinimumResponse;
import com.preschool.backendpreschool.model.MaterialConsumptionWindow;
import com.preschool.backendpreschool.model.MaterialMovementType;
import com.preschool.backendpreschool.model.MaterialStatus;
import com.preschool.backendpreschool.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public List<MaterialResponse> getMaterials(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean includeDeleted
    ) {
        return materialService.getMaterials(search, category, status, lowStock, includeDeleted);
    }

    @GetMapping("/low-stock")
    public List<MaterialResponse> getLowStockMaterials() {
        return materialService.getLowStockMaterials();
    }

    @GetMapping("/{materialId}")
    public MaterialResponse getMaterialById(@PathVariable Long materialId) {
        return materialService.getMaterialById(materialId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse createMaterial(@Valid @RequestBody MaterialRequest request) {
        return materialService.createMaterial(request);
    }

    @PutMapping("/{materialId}")
    public MaterialResponse updateMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialRequest request,
            Authentication authentication
    ) {
        return materialService.updateMaterial(materialId, request, authentication.getName());
    }

    @DeleteMapping("/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(@PathVariable Long materialId) {
        materialService.deleteMaterial(materialId);
    }

    @PostMapping("/{materialId}/restore")
    public MaterialResponse restoreMaterial(@PathVariable Long materialId) {
        return materialService.restoreMaterial(materialId);
    }

    @GetMapping("/{materialId}/suggested-minimum")
    public MaterialSuggestedMinimumResponse getSuggestedMinimum(
            @PathVariable Long materialId,
            @RequestParam(required = false) MaterialConsumptionWindow window
    ) {
        return materialService.getSuggestedMinimum(materialId, window);
    }

    @GetMapping("/{materialId}/audit-log")
    public List<MaterialAuditLogResponse> getAuditLog(@PathVariable Long materialId) {
        return materialService.getAuditLog(materialId);
    }

    @GetMapping("/movements")
    public List<MaterialMovementResponse> getMovements(@RequestParam(required = false) Long materialId) {
        return materialService.getMovements(materialId);
    }

    @GetMapping("/reports/movements")
    public List<MaterialMovementReportEntryResponse> getMovementsReport(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) MaterialMovementType type
    ) {
        return materialService.getMovementsReport(materialId, from, to, type);
    }

    @PostMapping("/{materialId}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialMovementResponse registerMovement(
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialMovementRequest request,
            Authentication authentication
    ) {
        return materialService.registerMovement(materialId, request, authentication.getName());
    }
}
