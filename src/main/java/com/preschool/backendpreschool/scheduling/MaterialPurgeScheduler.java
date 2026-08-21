package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaterialPurgeScheduler {

    private final MaterialService materialService;

    @Scheduled(cron = "0 15 3 * * *")
    public void purgeExpiredSoftDeletedMaterials() {
        materialService.purgeExpiredSoftDeletedMaterials();
    }
}
