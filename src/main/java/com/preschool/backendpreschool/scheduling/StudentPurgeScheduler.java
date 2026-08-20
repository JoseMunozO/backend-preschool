package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentPurgeScheduler {

    private final StudentService studentService;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredSoftDeletedStudents() {
        studentService.purgeExpiredSoftDeletedStudents();
    }
}
