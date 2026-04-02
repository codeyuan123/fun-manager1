package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime estimateTime,
        BigDecimal sourceRate,
        BigDecimal currentRate,
        BigDecimal minPurchaseAmount,
        List<FundReturnStatVO> returnStats,
        FundPerformanceRadarVO performanceRadar,
        List<FundManagerCardVO> managers,
        FundChartBlockVO assetAllocation,
        FundChartBlockVO holderStructure,
        List<FundScalePointVO> scaleTrend,
        List<FundPeerReferenceVO> sameTypeReferences
) {
}
