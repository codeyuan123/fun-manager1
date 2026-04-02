package com.fundmanager.scheduler;

import com.fundmanager.repository.FundPositionRepository;
import com.fundmanager.repository.FundWatchlistRepository;
import com.fundmanager.service.FundQuoteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class QuoteSyncScheduler {

    private final FundPositionRepository positionRepository;
    private final FundWatchlistRepository watchlistRepository;
    private final FundQuoteService quoteService;

    public QuoteSyncScheduler(FundPositionRepository positionRepository,
                              FundWatchlistRepository watchlistRepository,
                              FundQuoteService quoteService) {
        this.positionRepository = positionRepository;
        this.watchlistRepository = watchlistRepository;
        this.quoteService = quoteService;
    }

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Shanghai")
    public void syncQuote() {
        Set<String> codes = new HashSet<>();
        positionRepository.findAll().forEach(it -> codes.add(it.getFundCode()));
        watchlistRepository.findAll().forEach(it -> codes.add(it.getFundCode()));
        codes.forEach(quoteService::refreshEstimate);
    }
}
