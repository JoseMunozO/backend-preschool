package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.ScheduleSlotRequest;
import com.preschool.backendpreschool.dto.ScheduleSlotResponse;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentRequest;
import com.preschool.backendpreschool.dto.StaffGroupAssignmentResponse;
import com.preschool.backendpreschool.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleSlotResponse> getScheduleSlots(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) Boolean includeDeleted
    ) {
        return scheduleService.getScheduleSlots(groupId, dayOfWeek, includeDeleted);
    }

    @GetMapping("/{scheduleSlotId}")
    public ScheduleSlotResponse getScheduleSlotById(@PathVariable Long scheduleSlotId) {
        return scheduleService.getScheduleSlotById(scheduleSlotId);
    }

    @GetMapping("/groups/{groupId}")
    public List<ScheduleSlotResponse> getScheduleByGroup(@PathVariable Long groupId) {
        return scheduleService.getScheduleSlots(groupId, null, null);
    }

    @GetMapping("/days/{dayOfWeek}")
    public List<ScheduleSlotResponse> getScheduleByDay(@PathVariable DayOfWeek dayOfWeek) {
        return scheduleService.getScheduleSlots(null, dayOfWeek, null);
    }

    @GetMapping("/groups/{groupId}/days/{dayOfWeek}")
    public List<ScheduleSlotResponse> getScheduleByGroupAndDay(
            @PathVariable Long groupId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        return scheduleService.getScheduleSlots(groupId, dayOfWeek, null);
    }

    @DeleteMapping("/{scheduleSlotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScheduleSlot(@PathVariable Long scheduleSlotId) {
        scheduleService.deleteScheduleSlot(scheduleSlotId);
    }

    @PostMapping("/{scheduleSlotId}/restore")
    public ScheduleSlotResponse restoreScheduleSlot(@PathVariable Long scheduleSlotId) {
        return scheduleService.restoreScheduleSlot(scheduleSlotId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleSlotResponse createScheduleSlot(@Valid @RequestBody ScheduleSlotRequest request) {
        return scheduleService.createScheduleSlot(request);
    }

    @PutMapping("/{scheduleSlotId}")
    public ScheduleSlotResponse updateScheduleSlot(
            @PathVariable Long scheduleSlotId,
            @Valid @RequestBody ScheduleSlotRequest request
    ) {
        return scheduleService.updateScheduleSlot(scheduleSlotId, request);
    }

    @PutMapping("/{scheduleSlotId}/primary-staff/{staffId}")
    public ScheduleSlotResponse assignPrimaryStaff(
            @PathVariable Long scheduleSlotId,
            @PathVariable Long staffId
    ) {
        return scheduleService.assignPrimaryStaff(scheduleSlotId, staffId);
    }

    @GetMapping("/staff-assignments")
    public List<StaffGroupAssignmentResponse> getStaffGroupAssignments(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long staffId
    ) {
        return scheduleService.getStaffGroupAssignments(groupId, staffId);
    }

    @PostMapping("/staff-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffGroupAssignmentResponse assignStaffToGroup(
            @Valid @RequestBody StaffGroupAssignmentRequest request
    ) {
        return scheduleService.assignStaffToGroup(request);
    }
}
