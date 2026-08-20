package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ChargeTypeResponse;
import com.preschool.backendpreschool.dto.PaymentAllocationRequest;
import com.preschool.backendpreschool.dto.PaymentAllocationResponse;
import com.preschool.backendpreschool.dto.PaymentMonthlyReportResponse;
import com.preschool.backendpreschool.dto.PaymentRequest;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.dto.StudentChargeRequest;
import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.PaymentAllocation;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
import com.preschool.backendpreschool.repository.ChargeTypeRepository;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.PaymentAllocationRepository;
import com.preschool.backendpreschool.repository.PaymentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentChargeRepository;
import com.preschool.backendpreschool.repository.StudentGuardianRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final StudentChargeRepository studentChargeRepository;
    private final ChargeTypeRepository chargeTypeRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StaffRepository staffRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public List<ChargeTypeResponse> getChargeTypes(Boolean activeOnly) {
        List<ChargeType> chargeTypes = Boolean.TRUE.equals(activeOnly)
                ? chargeTypeRepository.findByActiveTrue()
                : chargeTypeRepository.findAll();

        return chargeTypes.stream()
                .map(this::toChargeTypeResponse)
                .toList();
    }

    public List<StudentChargeResponse> getCharges(Long studentId, StudentChargeStatus status, YearMonth month) {
        return studentChargeRepository.findAll()
                .stream()
                .filter(charge -> studentId == null || charge.getStudent().getStudentId().equals(studentId))
                .filter(charge -> status == null || charge.getStatus() == status)
                .filter(charge -> month == null || isInBillingMonth(charge, month))
                .map(this::toStudentChargeResponse)
                .toList();
    }

    public List<StudentChargeResponse> getCurrentParentCharges(String email) {
        Parent parent = parentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de padre/tutor no encontrado"));

        List<Long> studentIds = studentGuardianRepository.findByParentParentId(parent.getParentId())
                .stream()
                .map(guardian -> guardian.getStudent().getStudentId())
                .toList();

        return studentChargeRepository.findAll()
                .stream()
                .filter(charge -> studentIds.contains(charge.getStudent().getStudentId()))
                .map(this::toStudentChargeResponse)
                .toList();
    }

    public StudentChargeResponse getChargeById(Long studentChargeId) {
        return toStudentChargeResponse(findCharge(studentChargeId));
    }

    @Transactional
    public StudentChargeResponse createCharge(StudentChargeRequest request, String createdByEmail) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        ChargeType chargeType = chargeTypeRepository.findById(request.chargeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cargo no encontrado"));

        StudentCharge charge = StudentCharge.builder()
                .student(student)
                .chargeType(chargeType)
                .dueDate(request.dueDate())
                .billingPeriodStart(request.billingPeriodStart())
                .billingPeriodEnd(request.billingPeriodEnd())
                .amountDue(request.amountDue())
                .status(request.status() != null ? request.status() : StudentChargeStatus.PENDING)
                .description(trimToNull(request.description()))
                .build();

        return toStudentChargeResponse(studentChargeRepository.save(charge));
    }

    public List<PaymentResponse> getPayments(Long parentId, LocalDate dateFrom, LocalDate dateTo) {
        return paymentRepository.findAll()
                .stream()
                .filter(payment -> parentId == null || (payment.getParent() != null && payment.getParent().getParentId().equals(parentId)))
                .filter(payment -> dateFrom == null || !payment.getPaymentDate().isBefore(dateFrom))
                .filter(payment -> dateTo == null || !payment.getPaymentDate().isAfter(dateTo))
                .map(this::toPaymentResponse)
                .toList();
    }

    public PaymentMonthlyReportResponse getMonthlyReport(YearMonth month) {
        List<StudentChargeResponse> pendingCharges = studentChargeRepository.findAll()
                .stream()
                .filter(charge -> isInBillingMonth(charge, month))
                .filter(charge -> charge.getStatus() == StudentChargeStatus.PENDING
                        || charge.getStatus() == StudentChargeStatus.PARTIALLY_PAID)
                .map(this::toStudentChargeResponse)
                .toList();

        List<StudentChargeResponse> overdueCharges = studentChargeRepository.findAll()
                .stream()
                .filter(charge -> isInBillingMonth(charge, month))
                .filter(charge -> charge.getStatus() == StudentChargeStatus.OVERDUE)
                .map(this::toStudentChargeResponse)
                .toList();

        return new PaymentMonthlyReportResponse(
                month,
                pendingCharges.size(),
                sumBalance(pendingCharges),
                pendingCharges,
                overdueCharges.size(),
                sumBalance(overdueCharges),
                overdueCharges,
                totalPaymentsReceived(month)
        );
    }

    public List<PaymentResponse> getCurrentParentPayments(String email) {
        Parent parent = parentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de padre/tutor no encontrado"));

        return paymentRepository.findByParentParentId(parent.getParentId())
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    public PaymentResponse getPaymentById(Long paymentId) {
        return toPaymentResponse(findPayment(paymentId));
    }

    public List<PaymentResponse> getPaymentsByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Estudiante no encontrado");
        }

        return paymentRepository.findAll()
                .stream()
                .filter(payment -> paymentAllocationRepository.findByPaymentPaymentId(payment.getPaymentId())
                        .stream()
                        .anyMatch(allocation -> allocation.getStudentCharge().getStudent().getStudentId().equals(studentId)))
                .map(this::toPaymentResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        validatePaymentTotal(request);

        Parent parent = request.parentId() == null ? null : parentRepository.findById(request.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor no encontrado"));

        Staff staff = request.receivedByStaffId() == null ? null : staffRepository.findById(request.receivedByStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        Payment payment = Payment.builder()
                .parent(parent)
                .receivedByStaff(staff)
                .paymentDate(request.paymentDate())
                .totalAmount(request.totalAmount())
                .paymentMethod(request.paymentMethod())
                .referenceNumber(trimToNull(request.referenceNumber()))
                .notes(trimToNull(request.notes()))
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        Map<Long, StudentCharge> charges = loadCharges(request.allocations());

        for (PaymentAllocationRequest allocationRequest : request.allocations()) {
            StudentCharge charge = charges.get(allocationRequest.studentChargeId());
            validateChargeCanReceivePayment(charge);
            validateAllocationDoesNotOverpay(charge, allocationRequest.amountAllocated());

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(savedPayment)
                    .studentCharge(charge)
                    .amountAllocated(allocationRequest.amountAllocated())
                    .build();

            paymentAllocationRepository.save(allocation);
            updateChargeStatus(charge);
        }

        return toPaymentResponse(savedPayment);
    }

    private void validatePaymentTotal(PaymentRequest request) {
        BigDecimal allocatedTotal = request.allocations()
                .stream()
                .map(PaymentAllocationRequest::amountAllocated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocatedTotal.compareTo(request.totalAmount()) != 0) {
            throw new BadRequestException("La suma de asignaciones debe coincidir con el total del pago");
        }

        long distinctCharges = request.allocations()
                .stream()
                .map(PaymentAllocationRequest::studentChargeId)
                .distinct()
                .count();

        if (distinctCharges != request.allocations().size()) {
            throw new BadRequestException("No se puede asignar el mismo cargo mas de una vez en el mismo pago");
        }
    }

    private Map<Long, StudentCharge> loadCharges(List<PaymentAllocationRequest> allocations) {
        return allocations.stream()
                .map(PaymentAllocationRequest::studentChargeId)
                .distinct()
                .map(this::findCharge)
                .collect(Collectors.toMap(StudentCharge::getStudentChargeId, Function.identity()));
    }

    private void validateChargeCanReceivePayment(StudentCharge charge) {
        if (charge.getStatus() == StudentChargeStatus.CANCELLED) {
            throw new BadRequestException("No se puede pagar un cargo cancelado");
        }
    }

    private void validateAllocationDoesNotOverpay(StudentCharge charge, BigDecimal amountAllocated) {
        BigDecimal alreadyPaid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());
        BigDecimal remaining = charge.getAmountDue().subtract(alreadyPaid);

        if (amountAllocated.compareTo(remaining) > 0) {
            throw new BadRequestException("La asignacion supera el saldo pendiente del cargo");
        }
    }

    private void updateChargeStatus(StudentCharge charge) {
        BigDecimal paid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());

        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            charge.setStatus(charge.getDueDate().isBefore(LocalDate.now()) ? StudentChargeStatus.OVERDUE : StudentChargeStatus.PENDING);
        } else if (paid.compareTo(charge.getAmountDue()) >= 0) {
            charge.setStatus(StudentChargeStatus.PAID);
        } else {
            charge.setStatus(StudentChargeStatus.PARTIALLY_PAID);
        }

        studentChargeRepository.save(charge);
    }

    private boolean isInBillingMonth(StudentCharge charge, YearMonth month) {
        if (charge.getBillingPeriodStart() != null) {
            return YearMonth.from(charge.getBillingPeriodStart()).equals(month);
        }
        return YearMonth.from(charge.getDueDate()).equals(month);
    }

    private BigDecimal sumBalance(List<StudentChargeResponse> charges) {
        return charges.stream()
                .map(StudentChargeResponse::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPaymentsReceived(YearMonth month) {
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        return paymentRepository.findByPaymentDateBetween(firstDay, lastDay)
                .stream()
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    }

    private StudentCharge findCharge(Long studentChargeId) {
        return studentChargeRepository.findById(studentChargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo no encontrado"));
    }

    private ChargeTypeResponse toChargeTypeResponse(ChargeType chargeType) {
        return new ChargeTypeResponse(
                chargeType.getChargeTypeId(),
                chargeType.getCode(),
                chargeType.getName(),
                chargeType.getRecurrenceType(),
                chargeType.getDefaultAmount(),
                chargeType.getActive(),
                chargeType.getCreatedAt(),
                chargeType.getUpdatedAt()
        );
    }

    private StudentChargeResponse toStudentChargeResponse(StudentCharge charge) {
        BigDecimal paid = paymentAllocationRepository.sumAllocatedByStudentChargeId(charge.getStudentChargeId());
        BigDecimal balance = charge.getAmountDue().subtract(paid);
        Student student = charge.getStudent();
        ChargeType chargeType = charge.getChargeType();

        return new StudentChargeResponse(
                charge.getStudentChargeId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                chargeType.getChargeTypeId(),
                chargeType.getCode(),
                chargeType.getName(),
                charge.getDueDate(),
                charge.getBillingPeriodStart(),
                charge.getBillingPeriodEnd(),
                charge.getAmountDue(),
                paid,
                balance.max(BigDecimal.ZERO),
                charge.getStatus(),
                charge.getDescription(),
                charge.getCreatedAt(),
                charge.getUpdatedAt()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        Parent parent = payment.getParent();
        Staff staff = payment.getReceivedByStaff();

        List<PaymentAllocationResponse> allocations = paymentAllocationRepository.findByPaymentPaymentId(payment.getPaymentId())
                .stream()
                .map(this::toPaymentAllocationResponse)
                .toList();

        return new PaymentResponse(
                payment.getPaymentId(),
                parent != null ? parent.getParentId() : null,
                parent != null ? parent.getFirstName() + " " + parent.getLastName() : null,
                staff != null ? staff.getStaffId() : null,
                staff != null ? staff.getFirstName() + " " + staff.getLastName() : null,
                payment.getPaymentDate(),
                payment.getTotalAmount(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                allocations
        );
    }

    private PaymentAllocationResponse toPaymentAllocationResponse(PaymentAllocation allocation) {
        StudentCharge charge = allocation.getStudentCharge();
        Student student = charge.getStudent();

        return new PaymentAllocationResponse(
                allocation.getPaymentAllocationId(),
                charge.getStudentChargeId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                allocation.getAmountAllocated(),
                allocation.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
