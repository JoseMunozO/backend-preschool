package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<RoleResponse> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/{code}")
    public RoleResponse getRoleByCode(@PathVariable RoleName code) {
        return roleService.getRoleByCode(code);
    }
}
