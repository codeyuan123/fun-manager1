package com.fundmanager.scheduler;

import com.fundmanager.service.EstimateRefreshService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuoteSyncScheduler {

    private final EstimateRefreshService estimateRefreshService;

    public QuoteSyncScheduler(EstimateRefreshService estimateRefreshService) {
        this.estimateRefreshService = estimateRefreshService;
    }

    @Scheduled(cron = "0 35,40,45,50,55 9 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncMorningQuote() {
        estimateRefreshService.refreshDefaultUniverse();
    }

    @Scheduled(cron = "0 */5 10 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncMorningQuoteExtended() {
        estimateRefreshService.refreshDefaultUniverse();
    }

    @Scheduled(cron = "0 0,5,10,15,20,25,30 11 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncMorningCloseQuote() {
        estimateRefreshService.refreshDefaultUniverse();
    }

    @Scheduled(cron = "0 5,10,15,20,25,30,35,40,45,50,55 13 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncAfternoonQuote() {
        estimateRefreshService.refreshDefaultUniverse();
    }

    @Scheduled(cron = "0 */5 14 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncAfternoonQuoteExtended() {
        estimateRefreshService.refreshDefaultUniverse();
    }

    @Scheduled(cron = "0 0 15 ? * MON-FRI", zone = "Asia/Shanghai")
    public void syncAfternoonCloseQuote() {
        estimateRefreshService.refreshDefaultUniverse();
    }
}
