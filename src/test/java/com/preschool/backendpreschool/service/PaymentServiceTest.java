package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ChargeTypeRequest;
import com.preschool.backendpreschool.dto.ChargeTypeResponse;
import com.preschool.backendpreschool.dto.PaymentAllocationRequest;
import com.preschool.backendpreschool.dto.PaymentMonthlyReportResponse;
import com.preschool.backendpreschool.dto.PaymentRequest;
import com.preschool.backendpreschool.dto.ChargeDiscountRequest;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.dto.StudentChargeRequest;
import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.PaymentAllocation;
import com.preschool.backendpreschool.model.PaymentMethod;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ChargeTypeRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.PaymentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private StudentChargeRepository studentChargeRepository;

    @Mock
    private ChargeTypeRepository chargeTypeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReceiptPdfService receiptPdfService;

    @Mock
    private ReceiptStorageService receiptStorageService;

    @Spy
    private ChargeAmountCalculator chargeAmountCalculator = new ChargeAmountCalculator();

    @InjectMocks
    private PaymentService paymentService;

    private final List<PaymentAllocation> savedAllocations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        savedAllocations.clear();
    }

    @Test
    void createChargeTypeSucceedsWithUniqueCode() {
        when(chargeTypeRepository.existsByCode("UNIFORM")).thenReturn(false);
        when(chargeTypeRepository.save(any(ChargeType.class))).thenAnswer(invocation -> {
            ChargeType chargeType = invocation.getArgument(0);
            chargeType.setChargeTypeId(5L);
            return chargeType;
        });

        ChargeTypeResponse response = paymentService.createChargeType(new ChargeTypeRequest(
                "UNIFORM", "Uniform fee", ChargeRecurrenceType.ONE_TIME, new BigDecimal("1200.00"), true
        ));

        assertThat(response.chargeTypeId()).isEqualTo(5L);
        assertThat(response.code()).isEqualTo("UNIFORM");
        assertThat(response.defaultAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    void createChargeTypeRejectsDuplicateCode() {
        when(chargeTypeRepository.existsByCode("MONTHLY_FEE")).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createChargeType(new ChargeTypeRequest(
                "MONTHLY_FEE", "Monthly fee", ChargeRecurrenceType.MONTHLY, new BigDecimal("6000.00"), true
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateChargeTypeChangesDefaultAmount() {
        ChargeType existing = ChargeType.builder()
                .chargeTypeId(1L)
                .code("MONTHLY_FEE")
                .name("Monthly fee")
                .recurrenceType(ChargeRecurrenceType.MONTHLY)
                .defaultAmount(new BigDecimal("6000.00"))
                .active(true)
                .build();

        when(chargeTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(chargeTypeRepository.save(any(ChargeType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChargeTypeResponse response = paymentService.updateChargeType(1L, new ChargeTypeRequest(
                "MONTHLY_FEE", "Monthly fee", ChargeRecurrenceType.MONTHLY, new BigDecimal("6500.00"), true
        ));

        assertThat(response.defaultAmount()).isEqualByComparingTo("6500.00");
    }

    @Test
    void updateChargeTypeForMissingIdThrowsNotFound() {
        when(chargeTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updateChargeType(99L, new ChargeTypeRequest(
                "X", "X", ChargeRecurrenceType.ONE_TIME, BigDecimal.TEN, true
        ))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPaymentMarksChargeAsPaidWhenAllocationCoversFullAmount() {
        Parent parent = Parent.builder()
                .parentId(1L)
                .firstName("Carolina")
                .lastName("Demo")
                .build();
        Staff staff = Staff.builder()
                .staffId(3L)
                .firstName("Finance")
                .lastName("User")
                .build();
        StudentCharge charge = buildCharge(new BigDecimal("1500.00"), StudentChargeStatus.PENDING);

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(staffRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(new BigDecimal("1500.00"))
                .thenReturn(new BigDecimal("1500.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(20L);
            return payment;
        });
        when(paymentAllocationRepository.save(any(PaymentAllocation.class))).thenAnswer(invocation -> {
            PaymentAllocation allocation = invocation.getArgument(0);
            allocation.setPaymentAllocationId(30L);
            savedAllocations.add(allocation);
            return allocation;
        });
        when(paymentAllocationRepository.findByPaymentPaymentId(20L)).thenReturn(savedAllocations);
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(new PaymentRequest(
                1L,
                3L,
                LocalDate.of(2026, 6, 5),
                new BigDecimal("1500.00"),
                PaymentMethod.CASH,
                "CASH-JUN-001",
                "Pago completo",
                List.of(new PaymentAllocationRequest(10L, new BigDecimal("1500.00")))
        ));

        assertThat(response.paymentId()).isEqualTo(20L);
        assertThat(response.allocations()).hasSize(1);
        assertThat(charge.getStatus()).isEqualTo(StudentChargeStatus.PAID);
    }

    @Test
    void createPaymentGeneratesAndStoresReceiptPdf() {
        Parent parent = Parent.builder().parentId(1L).firstName("Carolina").lastName("Demo").build();
        Staff staff = Staff.builder().staffId(3L).firstName("Finance").lastName("User").build();
        StudentCharge charge = buildCharge(new BigDecimal("1500.00"), StudentChargeStatus.PENDING);
        byte[] pdfBytes = "pdf-content".getBytes();

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(staffRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(new BigDecimal("1500.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getPaymentId() == null) {
                payment.setPaymentId(20L);
            }
            return payment;
        });
        when(paymentAllocationRepository.save(any(PaymentAllocation.class))).thenAnswer(invocation -> {
            PaymentAllocation allocation = invocation.getArgument(0);
            allocation.setPaymentAllocationId(30L);
            savedAllocations.add(allocation);
            return allocation;
        });
        when(paymentAllocationRepository.findByPaymentPaymentId(20L)).thenReturn(savedAllocations);
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptPdfService.generateReceipt(any(Payment.class), any())).thenReturn(pdfBytes);
        when(receiptStorageService.store(pdfBytes)).thenReturn("receipt-file.pdf");

        paymentService.createPayment(new PaymentRequest(
                1L,
                3L,
                LocalDate.of(2026, 6, 5),
                new BigDecimal("1500.00"),
                PaymentMethod.CASH,
                "CASH-JUN-002",
                "Pago completo",
                List.of(new PaymentAllocationRequest(10L, new BigDecimal("1500.00")))
        ));

        verify(receiptPdfService).generateReceipt(any(Payment.class), any());
        verify(receiptStorageService).store(pdfBytes);
        assertThat(savedAllocations).isNotEmpty();
    }

    @Test
    void getReceiptPdfReturnsStoredFileWhenAvailableWithoutRegenerating() {
        Parent parent = Parent.builder().parentId(1L).build();
        Payment payment = Payment.builder().paymentId(20L).parent(parent).receiptFileName("existing.pdf").build();
        User admin = buildUser(2L, "admin@school.com", RoleName.ADMIN);

        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(receiptStorageService.readIfExists("existing.pdf")).thenReturn("stored-bytes".getBytes());

        byte[] result = paymentService.getReceiptPdf(20L, "admin@school.com");

        assertThat(result).isEqualTo("stored-bytes".getBytes());
        verify(receiptPdfService, never()).generateReceipt(any(), any());
    }

    @Test
    void getReceiptPdfRegeneratesWhenStoredFileIsMissing() {
        Parent parent = Parent.builder().parentId(1L).build();
        Payment payment = Payment.builder().paymentId(20L).parent(parent).receiptFileName(null).build();
        User admin = buildUser(2L, "admin@school.com", RoleName.ADMIN);
        byte[] pdfBytes = "regenerated".getBytes();

        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("admin@school.com")).thenReturn(Optional.of(admin));
        when(paymentAllocationRepository.findByPaymentPaymentId(20L)).thenReturn(List.of());
        when(receiptPdfService.generateReceipt(payment, List.of())).thenReturn(pdfBytes);
        when(receiptStorageService.store(pdfBytes)).thenReturn("new-file.pdf");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        byte[] result = paymentService.getReceiptPdf(20L, "admin@school.com");

        assertThat(result).isEqualTo(pdfBytes);
        assertThat(payment.getReceiptFileName()).isEqualTo("new-file.pdf");
    }

    @Test
    void getReceiptPdfAllowsParentToDownloadOwnReceipt() {
        Parent parent = Parent.builder().parentId(1L).build();
        Payment payment = Payment.builder().paymentId(20L).parent(parent).receiptFileName("file.pdf").build();
        User parentUser = buildUser(4L, "parent@school.com", RoleName.PARENT);

        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("parent@school.com")).thenReturn(Optional.of(parentUser));
        when(parentRepository.findByUserEmail("parent@school.com")).thenReturn(Optional.of(parent));
        when(receiptStorageService.readIfExists("file.pdf")).thenReturn("bytes".getBytes());

        byte[] result = paymentService.getReceiptPdf(20L, "parent@school.com");

        assertThat(result).isEqualTo("bytes".getBytes());
    }

    @Test
    void getReceiptPdfRejectsParentAccessingAnotherFamilysReceipt() {
        Parent owner = Parent.builder().parentId(1L).build();
        Parent requesterParent = Parent.builder().parentId(2L).build();
        Payment payment = Payment.builder().paymentId(20L).parent(owner).receiptFileName("file.pdf").build();
        User parentUser = buildUser(4L, "other-parent@school.com", RoleName.PARENT);

        when(paymentRepository.findById(20L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("other-parent@school.com")).thenReturn(Optional.of(parentUser));
        when(parentRepository.findByUserEmail("other-parent@school.com")).thenReturn(Optional.of(requesterParent));

        assertThatThrownBy(() -> paymentService.getReceiptPdf(20L, "other-parent@school.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createPaymentRejectsOverpayment() {
        StudentCharge charge = buildCharge(new BigDecimal("1500.00"), StudentChargeStatus.PENDING);

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(20L);
            return payment;
        });
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(new BigDecimal("1200.00"));

        PaymentRequest request = new PaymentRequest(
                null,
                null,
                LocalDate.of(2026, 6, 5),
                new BigDecimal("500.00"),
                PaymentMethod.TRANSFER,
                "TRF-JUN-001",
                "Pago parcial",
                List.of(new PaymentAllocationRequest(10L, new BigDecimal("500.00")))
        );

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La asignacion supera el saldo pendiente del cargo");
    }

    @Test
    void getMonthlyReportSeparatesPendingAndOverdueForTheGivenMonth() {
        StudentCharge junePending = buildCharge(10L, new BigDecimal("1000.00"), StudentChargeStatus.PENDING, 2026, 6);
        StudentCharge juneOverdue = buildCharge(11L, new BigDecimal("500.00"), StudentChargeStatus.OVERDUE, 2026, 6);
        StudentCharge julyPending = buildCharge(12L, new BigDecimal("1000.00"), StudentChargeStatus.PENDING, 2026, 7);

        when(studentChargeRepository.findAll()).thenReturn(List.of(junePending, juneOverdue, julyPending));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(new BigDecimal("300.00"));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(11L)).thenReturn(BigDecimal.ZERO);

        Payment juneCardPayment = Payment.builder()
                .paymentId(50L)
                .totalAmount(new BigDecimal("1070.00"))
                .build();
        when(paymentRepository.findByPaymentDateBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(juneCardPayment));

        PaymentMonthlyReportResponse report = paymentService.getMonthlyReport(YearMonth.of(2026, 6));

        assertThat(report.month()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(report.pendingCount()).isEqualTo(1);
        assertThat(report.pendingBalance()).isEqualByComparingTo("700.00");
        assertThat(report.pendingCharges()).extracting("studentChargeId").containsExactly(10L);
        assertThat(report.overdueCount()).isEqualTo(1);
        assertThat(report.overdueBalance()).isEqualByComparingTo("500.00");
        assertThat(report.overdueCharges()).extracting("studentChargeId").containsExactly(11L);
        assertThat(report.paymentsReceived()).isEqualByComparingTo("1070.00");
    }

    @Test
    void updateChargeAppliesFieldsAndExplicitStatus() {
        StudentCharge charge = buildCharge(new BigDecimal("1500.00"), StudentChargeStatus.PENDING);
        Student student = charge.getStudent();
        ChargeType chargeType = charge.getChargeType();

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(chargeTypeRepository.findById(1L)).thenReturn(Optional.of(chargeType));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(BigDecimal.ZERO);

        StudentChargeRequest request = new StudentChargeRequest(
                5L,
                1L,
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                new BigDecimal("1600.00"),
                StudentChargeStatus.CANCELLED,
                "Cancelado por cambio de plan",
                null,
                null,
                null
        );

        StudentChargeResponse response = paymentService.updateCharge(10L, request);

        assertThat(response.status()).isEqualTo(StudentChargeStatus.CANCELLED);
        assertThat(response.amountDue()).isEqualByComparingTo("1600.00");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(response.description()).isEqualTo("Cancelado por cambio de plan");
    }

    @Test
    void updateChargeWithNullStatusPreservesCurrentStatus() {
        StudentCharge charge = buildCharge(new BigDecimal("1500.00"), StudentChargeStatus.PARTIALLY_PAID);
        Student student = charge.getStudent();
        ChargeType chargeType = charge.getChargeType();

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(chargeTypeRepository.findById(1L)).thenReturn(Optional.of(chargeType));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(new BigDecimal("500.00"));

        StudentChargeRequest request = new StudentChargeRequest(
                5L,
                1L,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1500.00"),
                null,
                "Nota actualizada",
                null,
                null,
                null
        );

        StudentChargeResponse response = paymentService.updateCharge(10L, request);

        assertThat(response.status()).isEqualTo(StudentChargeStatus.PARTIALLY_PAID);
        assertThat(response.description()).isEqualTo("Nota actualizada");
    }

    @Test
    void updateChargeNotFoundThrowsNotFound() {
        when(studentChargeRepository.findById(10L)).thenReturn(Optional.empty());

        StudentChargeRequest request = new StudentChargeRequest(
                5L, 1L, LocalDate.of(2026, 6, 5), null, null, new BigDecimal("100.00"), null, null, null, null, null
        );

        assertThatThrownBy(() -> paymentService.updateCharge(10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getChargesExposesPaymentIdsForAChargeWithAllocations() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PAID);
        Payment payment = Payment.builder().paymentId(30L).build();
        PaymentAllocation allocation = PaymentAllocation.builder().payment(payment).studentCharge(charge).amountAllocated(new BigDecimal("1000.00")).build();

        when(studentChargeRepository.findAll()).thenReturn(List.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(new BigDecimal("1000.00"));
        when(paymentAllocationRepository.findByStudentChargeStudentChargeId(10L)).thenReturn(List.of(allocation));

        List<StudentChargeResponse> result = paymentService.getCharges(null, null, null, null);

        assertThat(result).singleElement()
                .satisfies(response -> assertThat(response.paymentIds()).containsExactly(30L));
    }

    @Test
    void getChargesFiltersByHasDiscount() {
        StudentCharge discounted = buildCharge(new BigDecimal("800.00"), StudentChargeStatus.PENDING);
        discounted.setStudentChargeId(11L);
        discounted.setOriginalAmount(new BigDecimal("1000.00"));
        discounted.setDiscountType(DiscountType.PERCENTAGE);
        discounted.setDiscountValue(new BigDecimal("20"));
        discounted.setDiscountReason("Hermanos");
        StudentCharge plain = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);
        plain.setStudentChargeId(12L);

        when(studentChargeRepository.findAll()).thenReturn(List.of(discounted, plain));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(any())).thenReturn(BigDecimal.ZERO);

        List<StudentChargeResponse> onlyDiscounted = paymentService.getCharges(null, null, null, true);
        List<StudentChargeResponse> onlyPlain = paymentService.getCharges(null, null, null, false);
        List<StudentChargeResponse> all = paymentService.getCharges(null, null, null, null);

        assertThat(onlyDiscounted).extracting(StudentChargeResponse::studentChargeId).containsExactly(11L);
        assertThat(onlyPlain).extracting(StudentChargeResponse::studentChargeId).containsExactly(12L);
        assertThat(all).hasSize(2);
    }

    @Test
    void applyChargeDiscountReducesOnlyThatChargeAndCapturesOriginalAmount() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(BigDecimal.ZERO);

        StudentChargeResponse response = paymentService.applyChargeDiscount(
                10L, new ChargeDiscountRequest(DiscountType.PERCENTAGE, new BigDecimal("20"), "Hermanos"));

        assertThat(response.amountDue()).isEqualByComparingTo("800.00");
        assertThat(response.originalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.discountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(response.discountReason()).isEqualTo("Hermanos");
    }

    @Test
    void applyChargeDiscountTwiceRecomputesFromOriginalInsteadOfCompounding() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(BigDecimal.ZERO);

        paymentService.applyChargeDiscount(10L, new ChargeDiscountRequest(DiscountType.PERCENTAGE, new BigDecimal("20"), "Primer intento"));
        StudentChargeResponse second = paymentService.applyChargeDiscount(
                10L, new ChargeDiscountRequest(DiscountType.PERCENTAGE, new BigDecimal("10"), "Descuento correcto"));

        assertThat(second.amountDue()).isEqualByComparingTo("900.00");
        assertThat(second.originalAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void applyChargeDiscountRejectsPercentageAboveOneHundred() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));

        assertThatThrownBy(() -> paymentService.applyChargeDiscount(
                10L, new ChargeDiscountRequest(DiscountType.PERCENTAGE, new BigDecimal("150"), "Beca")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeChargeDiscountRestoresOriginalAmount() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);
        charge.setOriginalAmount(new BigDecimal("1000.00"));
        charge.setAmountDue(new BigDecimal("800.00"));
        charge.setDiscountType(DiscountType.PERCENTAGE);
        charge.setDiscountValue(new BigDecimal("20"));
        charge.setDiscountReason("Hermanos");

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(10L)).thenReturn(BigDecimal.ZERO);

        StudentChargeResponse response = paymentService.removeChargeDiscount(10L);

        assertThat(response.amountDue()).isEqualByComparingTo("1000.00");
        assertThat(response.originalAmount()).isNull();
        assertThat(response.discountType()).isNull();
    }

    @Test
    void removeChargeDiscountWithoutExistingDiscountThrowsBadRequest() {
        StudentCharge charge = buildCharge(new BigDecimal("1000.00"), StudentChargeStatus.PENDING);

        when(studentChargeRepository.findById(10L)).thenReturn(Optional.of(charge));

        assertThatThrownBy(() -> paymentService.removeChargeDiscount(10L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createChargeWithDiscountFieldsAppliesDiscountImmediately() {
        Student student = Student.builder().studentId(5L).firstName("Mateo").lastName("Garcia").build();
        ChargeType chargeType = ChargeType.builder().chargeTypeId(3L).code("FIELD_TRIP").name("Field trip")
                .recurrenceType(ChargeRecurrenceType.ONE_TIME).build();

        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(chargeTypeRepository.findById(3L)).thenReturn(Optional.of(chargeType));
        when(studentChargeRepository.save(any(StudentCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAllocationRepository.sumAllocatedByStudentChargeId(any())).thenReturn(BigDecimal.ZERO);

        StudentChargeRequest request = new StudentChargeRequest(
                5L, 3L, LocalDate.of(2026, 9, 15), null, null, new BigDecimal("500.00"), null, null,
                DiscountType.FIXED_AMOUNT, new BigDecimal("100.00"), "Hermanos"
        );

        StudentChargeResponse response = paymentService.createCharge(request, "admin@school.com");

        assertThat(response.amountDue()).isEqualByComparingTo("400.00");
        assertThat(response.originalAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void createChargeRejectsPartialDiscountFields() {
        StudentChargeRequest request = new StudentChargeRequest(
                5L, 3L, LocalDate.of(2026, 9, 15), null, null, new BigDecimal("500.00"), null, null,
                DiscountType.FIXED_AMOUNT, null, null
        );

        assertThatThrownBy(() -> paymentService.createCharge(request, "admin@school.com"))
                .isInstanceOf(BadRequestException.class);
    }

    private StudentCharge buildCharge(Long chargeId, BigDecimal amountDue, StudentChargeStatus status, int year, int month) {
        Student student = Student.builder()
                .studentId(5L)
                .firstName("Mateo")
                .lastName("Garcia")
                .birthDate(LocalDate.of(2020, 5, 10))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2026, 1, 15))
                .build();
        ChargeType chargeType = ChargeType.builder()
                .chargeTypeId(1L)
                .code("MONTHLY_TUITION")
                .name("Cuota mensual")
                .recurrenceType(ChargeRecurrenceType.MONTHLY)
                .active(true)
                .build();

        return StudentCharge.builder()
                .studentChargeId(chargeId)
                .student(student)
                .chargeType(chargeType)
                .dueDate(LocalDate.of(year, month, 5))
                .billingPeriodStart(LocalDate.of(year, month, 1))
                .billingPeriodEnd(LocalDate.of(year, month, 28))
                .amountDue(amountDue)
                .status(status)
                .build();
    }

    private User buildUser(Long userId, String email, RoleName roleName) {
        return User.builder()
                .userId(userId)
                .email(email)
                .roles(Set.of(Role.builder().roleId(userId).code(roleName).name(roleName.name()).build()))
                .build();
    }

    private StudentCharge buildCharge(BigDecimal amountDue, StudentChargeStatus status) {
        Student student = Student.builder()
                .studentId(5L)
                .studentCode("STU-005")
                .firstName("Mateo")
                .lastName("Garcia")
                .birthDate(LocalDate.of(2020, 5, 10))
                .status(StudentStatus.active)
                .enrollmentDate(LocalDate.of(2026, 1, 15))
                .build();
        ChargeType chargeType = ChargeType.builder()
                .chargeTypeId(1L)
                .code("MONTHLY_TUITION")
                .name("Cuota mensual")
                .recurrenceType(ChargeRecurrenceType.MONTHLY)
                .defaultAmount(amountDue)
                .active(true)
                .build();

        return StudentCharge.builder()
                .studentChargeId(10L)
                .student(student)
                .chargeType(chargeType)
                .dueDate(LocalDate.of(2026, 6, 5))
                .billingPeriodStart(LocalDate.of(2026, 6, 1))
                .billingPeriodEnd(LocalDate.of(2026, 6, 30))
                .amountDue(amountDue)
                .status(status)
                .description("Cuota mensual junio 2026")
                .build();
    }
}
