package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.ParentRequest;
import com.preschool.backendpreschool.dto.ParentResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ParentResponse> getAllParents(ParentStatus status, String search) {
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

        return toResponse(parentRepository.save(parent));
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

    private Parent findParent(Long parentId) {
        return parentRepository.findById(parentId)
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
                parent.getUpdatedAt()
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
