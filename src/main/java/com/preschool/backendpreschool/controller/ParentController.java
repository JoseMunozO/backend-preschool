package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.ParentRequest;
import com.preschool.backendpreschool.dto.ParentResponse;
import com.preschool.backendpreschool.dto.StudentGuardianRequest;
import com.preschool.backendpreschool.dto.StudentGuardianResponse;
import com.preschool.backendpreschool.model.ParentStatus;
import com.preschool.backendpreschool.service.ParentService;
import com.preschool.backendpreschool.service.StudentGuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;
    private final StudentGuardianService studentGuardianService;

    @GetMapping
    public List<ParentResponse> getAllParents(
            @RequestParam(required = false) ParentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeDeleted,
            Authentication authentication
    ) {
        return parentService.getAllParents(status, search, includeDeleted, authentication.getName());
    }

    @GetMapping("/me")
    public ParentResponse getCurrentParent(Authentication authentication) {
        return parentService.getCurrentParent(authentication.getName());
    }

    @GetMapping("/me/students")
    public List<StudentGuardianResponse> getCurrentParentStudents(Authentication authentication) {
        return studentGuardianService.getGuardiansByParentEmail(authentication.getName());
    }

    @GetMapping("/{parentId}")
    public ParentResponse getParentById(@PathVariable Long parentId) {
        return parentService.getParentById(parentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParentResponse createParent(@Valid @RequestBody ParentRequest request) {
        return parentService.createParent(request);
    }

    @PutMapping("/{parentId}")
    public ParentResponse updateParent(
            @PathVariable Long parentId,
            @Valid @RequestBody ParentRequest request
    ) {
        return parentService.updateParent(parentId, request);
    }

    @PatchMapping("/{parentId}/activate")
    public ParentResponse activateParent(@PathVariable Long parentId) {
        return parentService.activateParent(parentId);
    }

    @PatchMapping("/{parentId}/deactivate")
    public ParentResponse deactivateParent(@PathVariable Long parentId) {
        return parentService.deactivateParent(parentId);
    }

    @DeleteMapping("/{parentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteParent(@PathVariable Long parentId) {
        parentService.deleteParent(parentId);
    }

    @PostMapping("/{parentId}/restore")
    public ParentResponse restoreParent(@PathVariable Long parentId) {
        return parentService.restoreParent(parentId);
    }

    @PostMapping("/{parentId}/claim")
    public ParentResponse claimParent(
            @PathVariable Long parentId,
            @Valid @RequestBody ParentRequest request
    ) {
        return parentService.claimParent(parentId, request);
    }

    @GetMapping("/{parentId}/students")
    public List<StudentGuardianResponse> getLinkedStudents(@PathVariable Long parentId) {
        return studentGuardianService.getGuardiansByParent(parentId);
    }

    @PostMapping("/{parentId}/students")
    public StudentGuardianResponse linkStudent(
            @PathVariable Long parentId,
            @Valid @RequestBody StudentGuardianRequest request
    ) {
        return studentGuardianService.linkStudent(parentId, request);
    }

    @DeleteMapping("/{parentId}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkStudent(
            @PathVariable Long parentId,
            @PathVariable Long studentId
    ) {
        studentGuardianService.unlinkStudent(parentId, studentId);
    }
}
