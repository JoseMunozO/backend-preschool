package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.AssignRoleRequest;
import com.preschool.backendpreschool.dto.CreateUserRequest;
import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.dto.UserResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
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

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("El email ya existe");
        }

        Set<Role> roles = request.roles()
                .stream()
                .map(this::findRole)
                .collect(Collectors.toSet());

        User user = User.builder()
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status("active")
                .emailVerified(false)
                .phoneVerified(false)
                .roles(roles)
                .build();

        return toResponse(userRepository.save(user));
    }

    public UserResponse assignRole(Long userId, AssignRoleRequest request) {
        User user = findUser(userId);

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(findRole(request.role()));
        user.setRoles(roles);

        return toResponse(userRepository.save(user));
    }

    public UserResponse removeRole(Long userId, AssignRoleRequest request) {
        User user = findUser(userId);

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.removeIf(role -> role.getCode() == request.role());
        user.setRoles(roles);

        return toResponse(userRepository.save(user));
    }

    public UserResponse deactivateUser(Long userId) {
        User user = findUser(userId);
        user.setStatus("inactive");
        return toResponse(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByCode(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private UserResponse toResponse(User user) {
        Set<RoleResponse> roles = user.getRoles()
                .stream()
                .map(role -> new RoleResponse(
                        role.getRoleId(),
                        role.getCode(),
                        role.getName()
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
}
