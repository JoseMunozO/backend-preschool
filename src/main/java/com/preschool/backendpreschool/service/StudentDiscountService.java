package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.StudentDiscountRequest;
import com.preschool.backendpreschool.dto.StudentDiscountResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.DiscountType;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentDiscount;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.StudentDiscountRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentDiscountService {

    private final StudentDiscountRepository studentDiscountRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<StudentDiscountResponse> getDiscounts(Long studentId) {
        findStudent(studentId);
        return studentDiscountRepository.findByStudentStudentIdOrderByValidFromDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentDiscountResponse createDiscount(Long studentId, StudentDiscountRequest request, String requesterEmail) {
        Student student = findStudent(studentId);
        User requester = findUser(requesterEmail);

        if (request.discountType() == DiscountType.PERCENTAGE && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje de descuento no puede ser mayor a 100");
        }
        if (request.validUntil() != null && request.validUntil().isBefore(request.validFrom())) {
            throw new BadRequestException("La fecha 'validUntil' no puede ser anterior a 'validFrom'");
        }

        StudentDiscount discount = StudentDiscount.builder()
                .student(student)
                .discountType(request.discountType())
                .value(request.value())
                .reason(trimToNull(request.reason()))
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .active(true)
                .createdByUser(requester)
                .build();

        return toResponse(studentDiscountRepository.save(discount));
    }

    @Transactional
    public StudentDiscountResponse deactivateDiscount(Long studentId, Long discountId) {
        StudentDiscount discount = studentDiscountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado"));

        if (!discount.getStudent().getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Descuento no encontrado");
        }

        discount.setActive(false);
        return toResponse(studentDiscountRepository.save(discount));
    }

    /**
     * Currently-valid discount for a student on a given date, used when generating charges.
     * If more than one happens to be valid at once, the most recently started one wins.
     */
    public Optional<StudentDiscount> findEffectiveDiscount(Long studentId, LocalDate date) {
        return studentDiscountRepository.findByStudentStudentIdAndActiveTrue(studentId)
                .stream()
                .filter(discount -> !discount.getValidFrom().isAfter(date)
                        && (discount.getValidUntil() == null || !discount.getValidUntil().isBefore(date)))
                .max(Comparator.comparing(StudentDiscount::getValidFrom));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findByStudentIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private StudentDiscountResponse toResponse(StudentDiscount discount) {
        Student student = discount.getStudent();
        User createdBy = discount.getCreatedByUser();

        return new StudentDiscountResponse(
                discount.getStudentDiscountId(),
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                discount.getDiscountType(),
                discount.getValue(),
                discount.getReason(),
                discount.getValidFrom(),
                discount.getValidUntil(),
                discount.getActive(),
                createdBy != null ? createdBy.getUserId() : null,
                createdBy != null ? createdBy.getEmail() : null,
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
