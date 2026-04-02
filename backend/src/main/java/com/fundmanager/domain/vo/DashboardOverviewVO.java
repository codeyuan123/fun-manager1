package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record DashboardOverviewVO(
        BigDecimal totalCost,
        BigDecimal totalMarketValue,
        BigDecimal totalEstimatedProfit,
        BigDecimal totalEstimatedProfitRate,
        BigDecimal totalTodayProfit,
        Integer fundCount
) {
}
