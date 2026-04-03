package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositionItemVO(
        String fundCode,
        String fundName,
        String fundType,
        BigDecimal totalShares,
        BigDecimal currentCost,
        BigDecimal currentNav,
        BigDecimal marketValue,
        BigDecimal estimatedProfit,
        BigDecimal estimatedProfitRate,
        BigDecimal todayProfit,
        LocalDate lastTradeDate,
        String estimateSource,
        String estimateConfidence,
        BigDecimal holdingCoverageRate,
        BigDecimal quotedCoverageRate,
        java.time.LocalDateTime estimateUpdatedAt
) {
}
