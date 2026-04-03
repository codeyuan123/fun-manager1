package com.fundmanager.service;

import com.fundmanager.domain.dto.EstimateRefreshRequest;
import com.fundmanager.domain.vo.EstimateRefreshSummaryVO;
import com.fundmanager.repository.FundPositionRepository;
import com.fundmanager.repository.FundWatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EstimateRefreshService {

    private final FundPositionRepository positionRepository;
    private final FundWatchlistRepository watchlistRepository;
    private final CurrentUserService currentUserService;
    private final FundQuoteService fundQuoteService;

    public EstimateRefreshService(FundPositionRepository positionRepository,
                                  FundWatchlistRepository watchlistRepository,
                                  CurrentUserService currentUserService,
                                  FundQuoteService fundQuoteService) {
        this.positionRepository = positionRepository;
        this.watchlistRepository = watchlistRepository;
        this.currentUserService = currentUserService;
        this.fundQuoteService = fundQuoteService;
    }

    public EstimateRefreshSummaryVO refreshForUser(String username, EstimateRefreshRequest request) {
        Long userId = currentUserService.getUserId(username);
        Set<String> codes = new LinkedHashSet<>();
        if (request != null && request.fundCodes() != null && !request.fundCodes().isEmpty()) {
            request.fundCodes().stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .forEach(codes::add);
        } else {
            positionRepository.findByUserIdOrderByFundCodeAsc(userId).forEach(item -> codes.add(item.getFundCode()));
            watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(item -> codes.add(item.getFundCode()));
        }
        return refreshCodes(codes);
    }

    public EstimateRefreshSummaryVO refreshDefaultUniverse() {
        Set<String> codes = new LinkedHashSet<>();
        positionRepository.findAll().forEach(item -> codes.add(item.getFundCode()));
        watchlistRepository.findAll().forEach(item -> codes.add(item.getFundCode()));
        return refreshCodes(codes);
    }

    private EstimateRefreshSummaryVO refreshCodes(Set<String> codes) {
        int success = 0;
        int failed = 0;
        for (String code : codes) {
            if (fundQuoteService.refreshEstimate(code).isPresent()) {
                success++;
            } else {
                failed++;
            }
        }
        return new EstimateRefreshSummaryVO(
                codes.size(),
                success,
                failed,
                LocalDateTime.now()
        );
    }
}
