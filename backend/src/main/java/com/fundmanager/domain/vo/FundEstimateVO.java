package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundEstimateVO(
        String fundCode,
        String fundName,
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime
) {
}
