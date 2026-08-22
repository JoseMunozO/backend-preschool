package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.StudentAttendanceBulkRequest;
import com.preschool.backendpreschool.dto.StudentAttendanceResponse;
import com.preschool.backendpreschool.service.StudentAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final StudentAttendanceService studentAttendanceService;

    @GetMapping
    public List<StudentAttendanceResponse> getAttendance(
            @RequestParam Long groupId,
            @RequestParam(required = false) LocalDate date,
            Authentication authentication
    ) {
        return studentAttendanceService.getAttendance(groupId, date != null ? date : LocalDate.now(), authentication.getName());
    }

    @PostMapping
    public List<StudentAttendanceResponse> saveAttendance(
            @Valid @RequestBody StudentAttendanceBulkRequest request,
            Authentication authentication
    ) {
        return studentAttendanceService.saveAttendance(request, authentication.getName());
    }
}
