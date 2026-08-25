package com.preschool.backendpreschool.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_charge_id")
    private Long studentChargeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_type_id", nullable = false)
    private ChargeType chargeType;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "amount_due", nullable = false)
    private BigDecimal amountDue;

    @Column(nullable = false, length = 20)
    @Convert(converter = StudentChargeStatusConverter.class)
    private StudentChargeStatus status;

    @Column(length = 255)
    private String description;

    /**
     * Set only once, the first time a discount is applied to this charge - lets a discount be
     * edited or removed later by recomputing from the true pre-discount amount, without needing a
     * separate history table for something this simple.
     */
    @Column(name = "original_amount")
    private BigDecimal originalAmount;

    @Convert(converter = DiscountTypeConverter.class)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
