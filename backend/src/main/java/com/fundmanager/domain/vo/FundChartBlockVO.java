package com.fundmanager.domain.vo;

import java.util.List;

public record FundChartBlockVO(
        List<String> categories,
        List<FundChartSeriesVO> series
) {
}
