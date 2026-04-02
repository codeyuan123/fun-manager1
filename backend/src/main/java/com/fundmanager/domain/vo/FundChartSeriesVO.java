package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public record FundChartSeriesVO(
        String name,
        List<BigDecimal> data
) {
}
