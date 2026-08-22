package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Role::getRankLevel).reversed().thenComparing(role -> role.getCode().name()))
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse getRoleByCode(RoleName code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        return toResponse(role);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getRankLevel(),
                role.getCreatedAt()
        );
    }
}
