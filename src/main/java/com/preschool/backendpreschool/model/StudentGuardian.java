package com.preschool.backendpreschool.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_guardians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardian {

    @EmbeddedId
    private StudentGuardianId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("parentId")
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "relationship_type", nullable = false, length = 30)
    @Convert(converter = GuardianRelationshipTypeConverter.class)
    private GuardianRelationshipType relationshipType;

    @Column(name = "is_primary_contact", nullable = false)
    private Boolean primaryContact;

    @Column(name = "is_billing_contact", nullable = false)
    private Boolean billingContact;

    @Column(name = "is_authorized_pickup", nullable = false)
    private Boolean authorizedPickup;

    @Column(name = "lives_with_student", nullable = false)
    private Boolean livesWithStudent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
