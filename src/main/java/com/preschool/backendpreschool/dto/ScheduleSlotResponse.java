package com.preschool.backendpreschool.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleSlotResponse(
        Long scheduleSlotId,
        Long groupId,
        String groupName,
        Long primaryStaffId,
        String primaryStaffName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String activityTitle,
        String roomName,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
