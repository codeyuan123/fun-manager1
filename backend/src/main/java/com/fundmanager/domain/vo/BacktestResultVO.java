package com.fundmanager.domain.vo;

import java.util.List;

public record BacktestResultVO(
        String fundCode,
        String fundName,
        String strategyCode,
        String strategyName,
        BacktestSummaryVO summary,
        List<BacktestSeriesPointVO> series
) {
}
