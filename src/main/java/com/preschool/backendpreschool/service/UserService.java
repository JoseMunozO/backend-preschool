package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.AssignRoleRequest;
import com.preschool.backendpreschool.dto.ChangeUserStatusRequest;
import com.preschool.backendpreschool.dto.CreateUserRequest;
import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.dto.UpdateUserRequest;
import com.preschool.backendpreschool.dto.UserResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers(UserStatus status, String search) {
        List<User> users;

        if (status != null) {
            users = userRepository.findByStatus(status);
        } else if (search != null && !search.isBlank()) {
            String term = search.trim();
            users = userRepository.findByEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(term, term);
        } else {
            users = userRepository.findAll();
        }

        return users
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request, String requesterEmail) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya existe");
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("El telefono ya existe");
        }

        Set<Role> roles = request.roles()
                .stream()
                .map(this::findRole)
                .collect(Collectors.toSet());

        User requester = findUser(requesterEmail);
        roles.forEach(role -> ensureCanAssignRole(requester, role));

        User user = User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .roles(roles)
                .build();

        return toResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

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

        return toResponse(userRepository.save(user));
    }

    public UserResponse assignRole(Long userId, AssignRoleRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        Role targetRole = findRole(request.role());
        ensureCanAssignRole(requester, targetRole);

        User user = findUser(userId);
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(targetRole);
        user.setRoles(roles);

        return toResponse(userRepository.save(user));
    }

    public UserResponse removeRole(Long userId, AssignRoleRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        Role targetRole = findRole(request.role());
        ensureCanAssignRole(requester, targetRole);

        User user = findUser(userId);

        if (targetRole.getCode() == RoleName.SUPER_ADMIN
                && user.getRoles().stream().anyMatch(role -> role.getCode() == RoleName.SUPER_ADMIN)
                && userRepository.countByRolesCode(RoleName.SUPER_ADMIN) <= 1) {
            throw new ConflictException("No se puede quitar el rol SUPER_ADMIN del ultimo usuario que lo tiene");
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.removeIf(role -> role.getCode() == request.role());

        if (roles.isEmpty()) {
            throw new BadRequestException("El usuario debe tener al menos un rol");
        }

        user.setRoles(roles);

        return toResponse(userRepository.save(user));
    }

    public UserResponse changeStatus(Long userId, ChangeUserStatusRequest request) {
        User user = findUser(userId);
        user.setStatus(request.status());
        return toResponse(userRepository.save(user));
    }

    public UserResponse deactivateUser(Long userId) {
        User user = findUser(userId);
        user.setStatus(UserStatus.INACTIVE);
        return toResponse(userRepository.save(user));
    }

    public UserResponse activateUser(Long userId) {
        User user = findUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        return toResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByCode(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private void ensureCanAssignRole(User requester, Role targetRole) {
        int requesterMaxRank = requester.getRoles().stream()
                .mapToInt(Role::getRankLevel)
                .max()
                .orElse(0);

        if (targetRole.getRankLevel() > requesterMaxRank) {
            throw new ForbiddenException("No puedes otorgar o quitar un rol de rango superior al tuyo");
        }
    }

    private UserResponse toResponse(User user) {
        Set<RoleResponse> roles = user.getRoles()
                .stream()
                .map(role -> new RoleResponse(
                        role.getRoleId(),
                        role.getCode(),
                        role.getName(),
                        role.getDescription(),
                        role.getRankLevel(),
                        role.getCreatedAt()
                ))
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getPhoneVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roles
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }
}
