package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FundNavVO(
        LocalDate navDate,
        BigDecimal unitNav,
        BigDecimal accumulatedNav,
        BigDecimal dailyGrowthRate
) {
}
