package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.StudentDiscountRequest;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.service.StudentDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments/students/{studentId}/discounts")
@RequiredArgsConstructor
public class StudentDiscountController {

    private final StudentDiscountService studentDiscountService;

    @GetMapping
    public List<StudentDiscountResponse> getDiscounts(@PathVariable Long studentId) {
        return studentDiscountService.getDiscounts(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDiscountResponse createDiscount(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentDiscountRequest request,
            Authentication authentication
    ) {
        return studentDiscountService.createDiscount(studentId, request, authentication.getName());
    }

    @PatchMapping("/{discountId}/deactivate")
    public StudentDiscountResponse deactivateDiscount(
            @PathVariable Long studentId,
            @PathVariable Long discountId
    ) {
        return studentDiscountService.deactivateDiscount(studentId, discountId);
    }
}
