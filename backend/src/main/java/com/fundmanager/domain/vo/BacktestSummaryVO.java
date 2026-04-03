package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record BacktestSummaryVO(
        BigDecimal cumulativeReturnRate,
        BigDecimal annualizedReturnRate,
        BigDecimal maxDrawdownRate,
        BigDecimal annualizedVolatility,
        BigDecimal sharpeRatio,
        BigDecimal calmarRatio,
        BigDecimal sortinoRatio,
        BigDecimal winRate,
        Integer recoveryDays,
        BigDecimal totalInvested,
        BigDecimal endingValue
) {
}
