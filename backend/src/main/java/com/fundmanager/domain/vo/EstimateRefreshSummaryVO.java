package com.fundmanager.domain.vo;

import java.time.LocalDateTime;

public record EstimateRefreshSummaryVO(
        Integer fundCount,
        Integer successCount,
        Integer failedCount,
        LocalDateTime refreshedAt
) {
}
