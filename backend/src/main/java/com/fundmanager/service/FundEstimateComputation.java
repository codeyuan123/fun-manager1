package com.fundmanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundEstimateComputation(
        String fundCode,
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateUpdatedAt,
        String estimateSource,
        String estimateConfidence,
        BigDecimal holdingCoverageRate,
        BigDecimal quotedCoverageRate,
        String rawSource
) {
}
