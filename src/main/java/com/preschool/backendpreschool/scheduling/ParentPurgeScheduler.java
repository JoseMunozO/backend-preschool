package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParentPurgeScheduler {

    private final ParentService parentService;

    @Scheduled(cron = "0 45 3 * * *")
    public void archiveExpiredSoftDeletedParents() {
        parentService.archiveExpiredSoftDeletedParents();
    }

    @Scheduled(cron = "0 50 3 * * *")
    public void purgeExpiredArchivedParents() {
        parentService.purgeExpiredArchivedParents();
    }
}
