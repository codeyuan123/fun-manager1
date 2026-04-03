package com.fundmanager.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class ThirdPartyFallbackEstimateStrategy implements FundEstimateStrategy {

    private final EastmoneyFundClient eastmoneyFundClient;
    private final EastmoneyFundParser eastmoneyFundParser;

    public ThirdPartyFallbackEstimateStrategy(EastmoneyFundClient eastmoneyFundClient,
                                              EastmoneyFundParser eastmoneyFundParser) {
        this.eastmoneyFundClient = eastmoneyFundClient;
        this.eastmoneyFundParser = eastmoneyFundParser;
    }

    @Override
    public boolean supports(FundEstimateContext context) {
        return true;
    }

    @Override
    public Optional<FundEstimateComputation> estimate(FundEstimateContext context) {
        try {
            EastmoneyEstimatePayload payload = eastmoneyFundParser.parseEstimateResponse(
                    context.fundCode(),
                    eastmoneyFundClient.estimate(context.fundCode())
            );
            if (payload.estimateNav() == null) {
                return Optional.empty();
            }
            return Optional.of(new FundEstimateComputation(
                    payload.fundCode(),
                    payload.estimateNav(),
                    scale(payload.estimateGrowthRate()),
                    payload.estimateTime(),
                    "third_party",
                    "fallback",
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    "fundgz.1234567.com.cn"
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }
}
