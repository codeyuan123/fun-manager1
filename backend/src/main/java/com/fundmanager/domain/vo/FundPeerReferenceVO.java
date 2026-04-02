package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record FundPeerReferenceVO(
        String fundCode,
        String fundName,
        BigDecimal returnRate
) {
}
