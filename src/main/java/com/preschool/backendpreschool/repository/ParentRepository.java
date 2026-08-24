package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.ParentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByUserUserId(Long userId);

    Optional<Parent> findByUserEmail(String email);

    boolean existsByUserUserId(Long userId);

    long countByStatus(ParentStatus status);

    List<Parent> findByStatus(ParentStatus status);

    List<Parent> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
            String firstName,
            String lastName,
            String email,
            String phone
    );

    Optional<Parent> findByParentIdAndDeletedAtIsNull(Long parentId);

    Optional<Parent> findByParentIdAndDeletedAtIsNotNull(Long parentId);

    Optional<Parent> findByParentIdAndArchivedAtIsNotNull(Long parentId);

    List<Parent> findAllByDeletedAtIsNotNullAndArchivedAtIsNullAndDeletedAtBefore(LocalDateTime cutoff);

    List<Parent> findAllByArchivedAtIsNotNullAndArchivedAtBefore(LocalDateTime cutoff);

    List<Parent> findAllByDeletedAtIsNotNullAndArchivedAtIsNull();

    List<Parent> findAllByArchivedAtIsNotNull();
}
