package com.fundmanager.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FundEstimateEngine {

    private final EquityHoldingEstimateStrategy equityHoldingEstimateStrategy;
    private final ThirdPartyFallbackEstimateStrategy thirdPartyFallbackEstimateStrategy;

    public FundEstimateEngine(EquityHoldingEstimateStrategy equityHoldingEstimateStrategy,
                              ThirdPartyFallbackEstimateStrategy thirdPartyFallbackEstimateStrategy) {
        this.equityHoldingEstimateStrategy = equityHoldingEstimateStrategy;
        this.thirdPartyFallbackEstimateStrategy = thirdPartyFallbackEstimateStrategy;
    }

    public Optional<FundEstimateComputation> estimate(FundEstimateContext context) {
        for (FundEstimateStrategy strategy : List.of(equityHoldingEstimateStrategy, thirdPartyFallbackEstimateStrategy)) {
            if (!strategy.supports(context)) {
                continue;
            }
            Optional<FundEstimateComputation> result = strategy.estimate(context);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
