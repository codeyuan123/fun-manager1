package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.dto.BacktestFundsRunRequest;
import com.fundmanager.domain.dto.BacktestStrategiesRunRequest;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.BacktestResultVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BacktestService {

    private final FundQuoteService fundQuoteService;
    private final BacktestStrategyEngine backtestStrategyEngine;
    private final BacktestExecutionService backtestExecutionService;

    public BacktestService(FundQuoteService fundQuoteService,
                           BacktestStrategyEngine backtestStrategyEngine,
                           BacktestExecutionService backtestExecutionService) {
        this.fundQuoteService = fundQuoteService;
        this.backtestStrategyEngine = backtestStrategyEngine;
        this.backtestExecutionService = backtestExecutionService;
    }

    public List<BacktestResultVO> runStrategies(BacktestStrategiesRunRequest request) {
        validateRequest(request.fundCode(), request.startDate(), request.endDate(), request.initialCapital());
        backtestStrategyEngine.validateStrategyCodes(request.strategyCodes());

        FundInfo fundInfo = ensureFundInfo(request.fundCode());
        List<FundNav> navs = loadNavs(request.fundCode(), request.startDate(), request.endDate());
        BigDecimal feeRate = currentFeeRate(request.fundCode());

        List<BacktestResultVO> result = new ArrayList<>();
        for (String strategyCode : request.strategyCodes()) {
            Map<String, Object> params = request.strategyParams() == null
                    ? Map.of()
                    : request.strategyParams().getOrDefault(strategyCode, Map.of());
            result.add(backtestExecutionService.execute(
                    request.fundCode(),
                    fundInfo.getFundName(),
                    navs,
                    feeRate,
                    strategyCode,
                    params,
                    request.startDate(),
                    request.endDate(),
                    request.executionMode(),
                    request.initialCapital()
            ));
        }
        return result;
    }

    public List<BacktestResultVO> runFunds(BacktestFundsRunRequest request) {
        validateRequest(null, request.startDate(), request.endDate(), request.initialCapital());
        backtestStrategyEngine.validateStrategyCodes(List.of(request.strategyCode()));
        List<String> fundCodes = request.fundCodes() == null
                ? List.of()
                : request.fundCodes().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (fundCodes.isEmpty()) {
            throw new BusinessException("At least one fund is required");
        }
        if (fundCodes.size() > 10) {
            throw new BusinessException("At most 10 funds can be compared");
        }

        List<BacktestResultVO> result = new ArrayList<>();
        for (String fundCode : fundCodes) {
            FundInfo fundInfo = ensureFundInfo(fundCode);
            List<FundNav> navs = loadNavs(fundCode, request.startDate(), request.endDate());
            result.add(backtestExecutionService.execute(
                    fundCode,
                    fundInfo.getFundName(),
                    navs,
                    currentFeeRate(fundCode),
                    request.strategyCode(),
                    request.strategyParams(),
                    request.startDate(),
                    request.endDate(),
                    request.executionMode(),
                    request.initialCapital()
            ));
        }
        return result;
    }

    private void validateRequest(String fundCode, LocalDate startDate, LocalDate endDate, BigDecimal initialCapital) {
        if (fundCode != null && fundCode.isBlank()) {
            throw new BusinessException("Fund code is required");
        }
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException("Invalid backtest date range");
        }
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Initial capital must be positive");
        }
    }

    private FundInfo ensureFundInfo(String fundCode) {
        fundQuoteService.refreshDetail(fundCode);
        return fundQuoteService.loadFundInfo(fundCode)
                .orElseGet(() -> {
                    FundInfo info = new FundInfo();
                    info.setFundCode(fundCode);
                    info.setFundName(fundCode);
                    info.setFundType("UNKNOWN");
                    info.setStatus((byte) 1);
                    info.setCreatedAt(LocalDateTime.now());
                    info.setUpdatedAt(LocalDateTime.now());
                    return info;
                });
    }

    private List<FundNav> loadNavs(String fundCode, LocalDate startDate, LocalDate endDate) {
        fundQuoteService.refreshDetail(fundCode);
        List<FundNav> navs = fundQuoteService.loadNavHistoryEntities(fundCode, startDate.minusDays(450), endDate);
        if (navs.stream().noneMatch(item -> !item.getNavDate().isBefore(startDate) && !item.getNavDate().isAfter(endDate))) {
            throw new BusinessException("No nav history found: " + fundCode);
        }
        return navs;
    }

    private BigDecimal currentFeeRate(String fundCode) {
        return fundQuoteService.refreshDetail(fundCode)
                .map(EastmoneyFundDetailPayload::currentRate)
                .orElse(BigDecimal.ZERO);
    }
}
