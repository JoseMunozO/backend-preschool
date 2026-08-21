package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSlotPurgeScheduler {

    private final ScheduleService scheduleService;

    @Scheduled(cron = "0 30 3 * * *")
    public void purgeExpiredSoftDeletedScheduleSlots() {
        scheduleService.purgeExpiredSoftDeletedScheduleSlots();
    }
}
