package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record RankingItemVO(
        String fundCode,
        String fundName,
        BigDecimal estimatedProfit,
        BigDecimal estimatedProfitRate
) {
}
