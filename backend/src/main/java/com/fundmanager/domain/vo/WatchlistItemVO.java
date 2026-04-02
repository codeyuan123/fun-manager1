package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistItemVO(
        String fundCode,
        String fundName,
        String fundType,
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime
) {
}
