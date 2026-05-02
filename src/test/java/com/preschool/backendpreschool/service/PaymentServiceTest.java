package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.PaymentAllocationRequest;
import com.preschool.backendpreschool.dto.PaymentRequest;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.model.ChargeRecurrenceType;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.PaymentAllocation;
import com.preschool.backendpreschool.model.PaymentMethod;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.repository.ChargeTypeRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.PaymentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private PaymentService paymentService;

    private final List<PaymentAllocation> savedAllocations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        savedAllocations.clear();
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
