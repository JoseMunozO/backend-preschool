package com.preschool.backendpreschool.scheduling;

import com.preschool.backendpreschool.service.MonthlyChargeGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class MonthlyChargeGenerationScheduler {

    private final MonthlyChargeGenerationService monthlyChargeGenerationService;

    @Scheduled(cron = "0 0 2 * * *")
    public void generateCurrentMonthCharges() {
        monthlyChargeGenerationService.generateMonthlyCharges(YearMonth.now());
    }
}
