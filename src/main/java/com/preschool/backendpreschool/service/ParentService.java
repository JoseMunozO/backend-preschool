package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ParentRequest;
import com.preschool.backendpreschool.dto.ParentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.ParentStatus;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.ParentRepository;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    static final int RESTORE_GRACE_PERIOD_DAYS = 7;
    static final int ARCHIVE_RETENTION_YEARS = 6;

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ParentResponse> getAllParents(ParentStatus status, String search, Boolean includeDeleted) {
        List<Parent> parents;

        if (status != null) {
            parents = parentRepository.findByStatus(status);
        } else if (search != null && !search.isBlank()) {
            String term = search.trim();
            parents = parentRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                    term,
                    term,
                    term,
                    term
            );
        } else {
            parents = parentRepository.findAll();
        }

        return parents.stream()
                .filter(parent -> Boolean.TRUE.equals(includeDeleted) || parent.getDeletedAt() == null)
                .map(this::toResponse)
                .toList();
    }

    public ParentResponse getParentById(Long parentId) {
        return toResponse(findParent(parentId));
    }

    public ParentResponse getCurrentParent(String email) {
        Parent parent = parentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de padre/tutor no encontrado"));

        return toResponse(parent);
    }

    @Transactional
    public ParentResponse createParent(ParentRequest request) {
        User user = createUserIfPasswordProvided(request);

        Parent parent = Parent.builder()
                .user(user)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizeEmail(request.email()))
                .phone(normalizePhone(request.phone()))
                .address(trimToNull(request.address()))
                .preferredLanguage(trimToNull(request.preferredLanguage()))
                .status(request.status() != null ? request.status() : ParentStatus.ACTIVE)
                .notes(trimToNull(request.notes()))
                .build();

        return toResponse(parentRepository.save(parent));
    }

    @Transactional
    public ParentResponse updateParent(Long parentId, ParentRequest request) {
        Parent parent = findParent(parentId);
        applyParentFields(parent, request);

        return toResponse(parentRepository.save(parent));
    }

    @Transactional
    public ParentResponse claimParent(Long parentId, ParentRequest request) {
        Parent parent = parentRepository.findByParentIdAndArchivedAtIsNotNull(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor archivado no encontrado"));

        LocalDateTime archiveWindowStart = LocalDateTime.now().minusYears(ARCHIVE_RETENTION_YEARS);
        if (parent.getArchivedAt().isBefore(archiveWindowStart)) {
            throw new ConflictException("La ventana de recuperación de " + ARCHIVE_RETENTION_YEARS + " años ya expiró");
        }

        applyParentFields(parent, request);
        parent.setDeletedAt(null);
        parent.setArchivedAt(null);

        return toResponse(parentRepository.save(parent));
    }

    private void applyParentFields(Parent parent, ParentRequest request) {
        User user = parent.getUser();

        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (user != null) {
            updateLinkedUser(user, email, phone, request.password());
        } else if (hasText(request.password())) {
            user = createParentUser(email, phone, request.password());
            parent.setUser(user);
        }

        parent.setFirstName(request.firstName().trim());
        parent.setLastName(request.lastName().trim());
        parent.setEmail(email);
        parent.setPhone(phone);
        parent.setAddress(trimToNull(request.address()));
        parent.setPreferredLanguage(trimToNull(request.preferredLanguage()));
        parent.setStatus(request.status() != null ? request.status() : ParentStatus.ACTIVE);
        parent.setNotes(trimToNull(request.notes()));
    }

    public ParentResponse deactivateParent(Long parentId) {
        Parent parent = findParent(parentId);
        parent.setStatus(ParentStatus.INACTIVE);

        if (parent.getUser() != null) {
            parent.getUser().setStatus(UserStatus.INACTIVE);
        }

        return toResponse(parentRepository.save(parent));
    }

    public ParentResponse activateParent(Long parentId) {
        Parent parent = findParent(parentId);
        parent.setStatus(ParentStatus.ACTIVE);

        if (parent.getUser() != null) {
            parent.getUser().setStatus(UserStatus.ACTIVE);
        }

        return toResponse(parentRepository.save(parent));
    }

    @Transactional
    public void deleteParent(Long parentId) {
        Parent parent = findParent(parentId);
        parent.setDeletedAt(LocalDateTime.now());
        parentRepository.save(parent);
    }

    @Transactional
    public ParentResponse restoreParent(Long parentId) {
        Parent parent = parentRepository.findByParentIdAndDeletedAtIsNotNull(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor eliminado no encontrado"));

        LocalDateTime graceWindowStart = LocalDateTime.now().minusDays(RESTORE_GRACE_PERIOD_DAYS);
        if (parent.getDeletedAt().isBefore(graceWindowStart)) {
            throw new ConflictException("La ventana de restauración de " + RESTORE_GRACE_PERIOD_DAYS + " días ya expiró");
        }

        parent.setDeletedAt(null);
        return toResponse(parentRepository.save(parent));
    }

    @Transactional
    public void archiveExpiredSoftDeletedParents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RESTORE_GRACE_PERIOD_DAYS);

        for (Parent parent : parentRepository.findAllByDeletedAtIsNotNullAndArchivedAtIsNullAndDeletedAtBefore(cutoff)) {
            parent.setPhone(null);
            parent.setAddress(null);
            parent.setPreferredLanguage(null);
            parent.setNotes(null);
            parent.setArchivedAt(LocalDateTime.now());
            parentRepository.save(parent);
        }
    }

    public void purgeExpiredArchivedParents() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(ARCHIVE_RETENTION_YEARS);

        for (Parent parent : parentRepository.findAllByArchivedAtIsNotNullAndArchivedAtBefore(cutoff)) {
            try {
                parentRepository.delete(parent);
            } catch (DataIntegrityViolationException ex) {
                log.warn("No se pudo purgar el padre/tutor archivado {}: tiene registros relacionados", parent.getParentId());
            }
        }
    }

    private Parent findParent(Long parentId) {
        return parentRepository.findByParentIdAndDeletedAtIsNull(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Padre/tutor no encontrado"));
    }

    private User createUserIfPasswordProvided(ParentRequest request) {
        if (!hasText(request.password())) {
            return null;
        }

        return createParentUser(
                normalizeEmail(request.email()),
                normalizePhone(request.phone()),
                request.password()
        );
    }

    private User createParentUser(String email, String phone, String password) {
        if (!hasText(email)) {
            throw new BadRequestException("El email es requerido para crear la cuenta de acceso del padre/tutor");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya existe");
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("El telefono ya existe");
        }

        Role parentRole = roleRepository.findByCode(RoleName.PARENT)
                .orElseThrow(() -> new ResourceNotFoundException("Rol PARENT no encontrado"));

        User user = User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .roles(Set.of(parentRole))
                .build();

        return userRepository.save(user);
    }

    private void updateLinkedUser(User user, String email, String phone, String password) {
        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya existe");
        }

        if (phone != null && !phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("El telefono ya existe");
        }

        if (email != null) {
            user.setEmail(email);
        }
        user.setPhone(phone);

        if (hasText(password)) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
    }

    private ParentResponse toResponse(Parent parent) {
        User user = parent.getUser();

        return new ParentResponse(
                parent.getParentId(),
                user != null ? user.getUserId() : null,
                parent.getFirstName(),
                parent.getLastName(),
                parent.getEmail(),
                parent.getPhone(),
                parent.getAddress(),
                parent.getPreferredLanguage(),
                parent.getStatus(),
                parent.getNotes(),
                parent.getCreatedAt(),
                parent.getUpdatedAt(),
                parent.getDeletedAt(),
                parent.getArchivedAt()
        );
    }

    private String normalizeEmail(String email) {
        return hasText(email) ? email.trim().toLowerCase() : null;
    }

    private String normalizePhone(String phone) {
        return hasText(phone) ? phone.trim() : null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
