package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundEstimateHistoryPointVO(
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime,
        String estimateSource,
        String estimateConfidence
) {
}
