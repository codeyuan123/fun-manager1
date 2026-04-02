package com.fundmanager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FundQuoteSnapshot(
        String fundCode,
        String fundName,
        BigDecimal currentNav,
        BigDecimal latestNav,
        LocalDate latestNavDate,
        BigDecimal previousNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime
) {
}
