package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public record FundPerformanceRadarVO(
        String average,
        List<String> categories,
        List<BigDecimal> data
) {
}
