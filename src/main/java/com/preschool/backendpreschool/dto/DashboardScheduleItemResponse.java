package com.preschool.backendpreschool.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DashboardScheduleItemResponse(
        Long scheduleSlotId,
        Long groupId,
        String groupName,
        Long primaryStaffId,
        String primaryStaffName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String activityTitle,
        String roomName
) {
}
