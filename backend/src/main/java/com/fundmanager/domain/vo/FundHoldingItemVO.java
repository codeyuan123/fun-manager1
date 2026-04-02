package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record FundHoldingItemVO(
        String stockCode,
        String stockName,
        BigDecimal navRatio,
        BigDecimal holdingShares,
        BigDecimal holdingMarketValue,
        String reportDate
) {
}
