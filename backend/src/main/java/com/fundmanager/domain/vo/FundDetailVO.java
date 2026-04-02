package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FundDetailVO(
        String fundCode,
        String fundName,
        String fundType,
        String riskLevel,
        String managementCompany,
        BigDecimal latestNav,
        LocalDate latestNavDate,
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime
) {
}
