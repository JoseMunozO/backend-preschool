package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.StudentConsent;
import com.preschool.backendpreschool.model.StudentConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentConsentRepository extends JpaRepository<StudentConsent, Long> {

    List<StudentConsent> findByStudentStudentIdOrderByCreatedAtDesc(Long studentId);

    List<StudentConsent> findByStudentStudentIdAndParentParentIdOrderByCreatedAtDesc(Long studentId, Long parentId);

    Optional<StudentConsent> findByStudentStudentIdAndParentParentIdAndConsentTypeAndRevokedAtIsNull(
            Long studentId,
            Long parentId,
            StudentConsentType consentType
    );

    boolean existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
            Long studentId,
            StudentConsentType consentType
    );
}
