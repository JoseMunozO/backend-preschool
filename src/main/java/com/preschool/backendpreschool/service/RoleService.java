package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.model.Role;
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
                .sorted(Comparator.comparing(role -> role.getCode().name()))
                .map(this::toResponse)
                .toList();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName()
        );
    }
}
