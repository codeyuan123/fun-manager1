package com.fundmanager.domain.vo;

public record FundManagerCardVO(
        String id,
        String name,
        String avatar,
        Integer star,
        String workTime,
        String fundSize,
        FundPerformanceRadarVO power,
        FundChartBlockVO profitComparison
) {
}
