package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ChargeDiscountRequest;
import com.preschool.backendpreschool.dto.ChargeTypeRequest;
import com.preschool.backendpreschool.dto.ChargeTypeResponse;
import com.preschool.backendpreschool.dto.PaymentAllocationRequest;
import com.preschool.backendpreschool.dto.PaymentAllocationResponse;
import com.preschool.backendpreschool.dto.PaymentMonthlyReportResponse;
import com.preschool.backendpreschool.dto.PaymentRequest;
import com.preschool.backendpreschool.dto.PaymentResponse;
import com.preschool.backendpreschool.dto.StudentChargeRequest;
import com.preschool.backendpreschool.dto.StudentChargeResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ChargeType;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.PaymentAllocation;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentCharge;
import com.preschool.backendpreschool.model.StudentChargeStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
    private final UserRepository userRepository;
    private final ReceiptPdfService receiptPdfService;
    private final ReceiptStorageService receiptStorageService;
    private final ChargeAmountCalculator chargeAmountCalculator;

    public List<ChargeTypeResponse> getChargeTypes(Boolean activeOnly) {
        List<ChargeType> chargeTypes = Boolean.TRUE.equals(activeOnly)
                ? chargeTypeRepository.findByActiveTrue()
                : chargeTypeRepository.findAll();

        return chargeTypes.stream()
                .map(this::toChargeTypeResponse)
                .toList();
    }

    @Transactional
    public ChargeTypeResponse createChargeType(ChargeTypeRequest request) {
        if (chargeTypeRepository.existsByCode(request.code())) {
            throw new BadRequestException("El codigo de tipo de cargo ya existe");
        }

        ChargeType chargeType = ChargeType.builder()
                .code(request.code().trim())
                .name(request.name().trim())
                .recurrenceType(request.recurrenceType())
                .defaultAmount(request.defaultAmount())
                .active(request.active() != null ? request.active() : true)
                .build();

        return toChargeTypeResponse(chargeTypeRepository.save(chargeType));
    }

    @Transactional
    public ChargeTypeResponse updateChargeType(Long chargeTypeId, ChargeTypeRequest request) {
        ChargeType chargeType = chargeTypeRepository.findById(chargeTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cargo no encontrado"));

        if (!chargeType.getCode().equalsIgnoreCase(request.code()) && chargeTypeRepository.existsByCode(request.code())) {
            throw new BadRequestException("El codigo de tipo de cargo ya existe");
        }

        chargeType.setCode(request.code().trim());
        chargeType.setName(request.name().trim());
        chargeType.setRecurrenceType(request.recurrenceType());
        chargeType.setDefaultAmount(request.defaultAmount());
        if (request.active() != null) {
            chargeType.setActive(request.active());
        }

        return toChargeTypeResponse(chargeTypeRepository.save(chargeType));
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
        validateDiscountFieldsTogether(request.discountType(), request.discountValue(), request.discountReason());

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

        if (request.discountType() != null) {
            applyDiscountToCharge(charge, request.discountType(), request.discountValue(), request.discountReason());
        }

        return toStudentChargeResponse(studentChargeRepository.save(charge));
    }

    @Transactional
    public StudentChargeResponse applyChargeDiscount(Long studentChargeId, ChargeDiscountRequest request) {
        StudentCharge charge = findCharge(studentChargeId);
        applyDiscountToCharge(charge, request.discountType(), request.value(), request.reason());
        updateChargeStatus(charge);

        return toStudentChargeResponse(charge);
    }

    @Transactional
    public StudentChargeResponse removeChargeDiscount(Long studentChargeId) {
        StudentCharge charge = findCharge(studentChargeId);
        if (charge.getOriginalAmount() == null) {
            throw new BadRequestException("Este cargo no tiene ningun descuento aplicado");
        }

        charge.setAmountDue(charge.getOriginalAmount());
        charge.setOriginalAmount(null);
        charge.setDiscountType(null);
        charge.setDiscountValue(null);
        charge.setDiscountReason(null);
        updateChargeStatus(charge);

        return toStudentChargeResponse(charge);
    }

    private void validateDiscountFieldsTogether(Object discountType, Object discountValue, Object discountReason) {
        boolean anyPresent = discountType != null || discountValue != null || discountReason != null;
        boolean allPresent = discountType != null && discountValue != null && discountReason != null;
        if (anyPresent && !allPresent) {
            throw new BadRequestException("discountType, discountValue y discountReason deben venir los tres juntos, o ninguno");
        }
    }

    /**
     * originalAmount is captured only the first time a discount is applied, so re-applying a
     * different discount later always recomputes from the true pre-discount amount instead of
     * compounding on top of an already-discounted amountDue.
     */
    private void applyDiscountToCharge(StudentCharge charge, DiscountType discountType, BigDecimal discountValue, String discountReason) {
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje de descuento no puede ser mayor a 100");
        }

        BigDecimal baseAmount = charge.getOriginalAmount() != null ? charge.getOriginalAmount() : charge.getAmountDue();

        charge.setOriginalAmount(baseAmount);
        charge.setDiscountType(discountType);
        charge.setDiscountValue(discountValue);
        charge.setDiscountReason(trimToNull(discountReason));
        charge.setAmountDue(chargeAmountCalculator.applyDiscount(baseAmount, discountType, discountValue));
    }

    @Transactional
    public StudentChargeResponse updateCharge(Long studentChargeId, StudentChargeRequest request) {
        StudentCharge charge = findCharge(studentChargeId);

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        ChargeType chargeType = chargeTypeRepository.findById(request.chargeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cargo no encontrado"));

        charge.setStudent(student);
        charge.setChargeType(chargeType);
        charge.setDueDate(request.dueDate());
        charge.setBillingPeriodStart(request.billingPeriodStart());
        charge.setBillingPeriodEnd(request.billingPeriodEnd());
        charge.setAmountDue(request.amountDue());
        charge.setDescription(trimToNull(request.description()));

        if (request.status() != null) {
            charge.setStatus(request.status());
        }

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

        try {
            generateAndStoreReceipt(savedPayment);
        } catch (Exception e) {
            log.warn("No se pudo generar el recibo PDF para el pago {}", savedPayment.getPaymentId(), e);
        }

        return toPaymentResponse(savedPayment);
    }

    public byte[] getReceiptPdf(Long paymentId, String requesterEmail) {
        Payment payment = findPayment(paymentId);
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        ensureCanAccessReceipt(payment, requester);

        byte[] existing = receiptStorageService.readIfExists(payment.getReceiptFileName());
        if (existing != null) {
            return existing;
        }

        return generateAndStoreReceipt(payment);
    }

    private byte[] generateAndStoreReceipt(Payment payment) {
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByPaymentPaymentId(payment.getPaymentId());
        byte[] pdfBytes = receiptPdfService.generateReceipt(payment, allocations);

        String filename = receiptStorageService.store(pdfBytes);
        payment.setReceiptFileName(filename);
        paymentRepository.save(payment);

        return pdfBytes;
    }

    private void ensureCanAccessReceipt(Payment payment, User requester) {
        if (hasStaffPaymentAccess(requester)) {
            return;
        }

        if (hasRole(requester, RoleName.PARENT)) {
            Parent parent = parentRepository.findByUserEmail(requester.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de padre/tutor no encontrado"));

            if (payment.getParent() != null && payment.getParent().getParentId().equals(parent.getParentId())) {
                return;
            }
        }

        throw new ForbiddenException("No tienes permiso para descargar este recibo");
    }

    private boolean hasStaffPaymentAccess(User user) {
        return hasRole(user, RoleName.SUPER_ADMIN)
                || hasRole(user, RoleName.ADMIN)
                || hasRole(user, RoleName.DIRECTOR)
                || hasRole(user, RoleName.FINANCE);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.getCode() == roleName);
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
                charge.getOriginalAmount(),
                charge.getDiscountType(),
                charge.getDiscountValue(),
                charge.getDiscountReason(),
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
