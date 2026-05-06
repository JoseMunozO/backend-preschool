package com.preschool.backendpreschool.dto;

import java.time.LocalDate;

public record DashboardBirthdayResponse(
        Long studentId,
        String studentName,
        LocalDate birthDate,
        LocalDate nextBirthday,
        int daysUntilBirthday
) {
}
