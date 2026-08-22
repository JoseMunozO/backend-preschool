package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.StaffRequest;
import com.preschool.backendpreschool.dto.StaffResponse;
import com.preschool.backendpreschool.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public List<StaffResponse> getAllStaff(@RequestParam(required = false) Boolean includeDeleted) {
        return staffService.getAllStaff(includeDeleted);
    }

    @GetMapping("/{staffId}")
    public StaffResponse getStaffById(@PathVariable Long staffId) {
        return staffService.getStaffById(staffId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@Valid @RequestBody StaffRequest request, Authentication authentication) {
        return staffService.createStaff(request, authentication.getName());
    }

    @DeleteMapping("/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStaff(@PathVariable Long staffId, Authentication authentication) {
        staffService.deleteStaff(staffId, authentication.getName());
    }

    @PostMapping("/{staffId}/restore")
    public StaffResponse restoreStaff(@PathVariable Long staffId) {
        return staffService.restoreStaff(staffId);
    }
}
