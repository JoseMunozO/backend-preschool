package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.ChargeTypeResponse;
import com.preschool.backendpreschool.dto.PaymentRequest;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.dto.StudentChargeRequest;
import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/charge-types")
    public List<ChargeTypeResponse> getChargeTypes(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        return paymentService.getChargeTypes(activeOnly);
    }

    @GetMapping("/charges")
    public List<StudentChargeResponse> getCharges(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) StudentChargeStatus status,
            @RequestParam(required = false) String month
    ) {
        return paymentService.getCharges(studentId, status, month != null ? YearMonth.parse(month) : null);
    }

    @GetMapping("/charges/{studentChargeId}")
    public StudentChargeResponse getChargeById(@PathVariable Long studentChargeId) {
        return paymentService.getChargeById(studentChargeId);
    }

    @PostMapping("/charges")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentChargeResponse createCharge(
            @Valid @RequestBody StudentChargeRequest request,
            Authentication authentication
    ) {
        return paymentService.createCharge(request, authentication.getName());
    }

    @GetMapping("/me")
    public List<PaymentResponse> getCurrentParentPayments(Authentication authentication) {
        return paymentService.getCurrentParentPayments(authentication.getName());
    }

    @GetMapping("/me/charges")
    public List<StudentChargeResponse> getCurrentParentCharges(Authentication authentication) {
        return paymentService.getCurrentParentCharges(authentication.getName());
    }

    @GetMapping
    public List<PaymentResponse> getPayments(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return paymentService.getPayments(parentId, dateFrom, dateTo);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPaymentById(@PathVariable Long paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping("/students/{studentId}")
    public List<PaymentResponse> getPaymentsByStudent(@PathVariable Long studentId) {
        return paymentService.getPaymentsByStudent(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }
}
