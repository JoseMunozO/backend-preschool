package com.preschool.backendpreschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleSlotRequest(
        @NotNull
        Long groupId,

        Long primaryStaffId,

        @NotNull
        DayOfWeek dayOfWeek,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime,

        @NotBlank
        @Size(max = 150)
        String activityTitle,

        @Size(max = 100)
        String roomName,

        String notes
) {
}
