package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaffAccountExpirationScheduler {

    private final StaffService staffService;

    @Scheduled(cron = "0 0 3 * * *")
    public void deactivateExpiredStaffAccounts() {
        staffService.deactivateExpiredStaffAccounts();
    }
}
