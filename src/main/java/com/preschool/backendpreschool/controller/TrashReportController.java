package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.TrashEntryResponse;
import com.preschool.backendpreschool.service.TrashReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports/trash")
@RequiredArgsConstructor
public class TrashReportController {

    private final TrashReportService trashReportService;

    @GetMapping
    public List<TrashEntryResponse> getTrash() {
        return trashReportService.getTrash();
    }
}
