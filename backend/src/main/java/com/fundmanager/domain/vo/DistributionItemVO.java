package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record DistributionItemVO(
        String category,
        BigDecimal cost,
        BigDecimal marketValue
) {
}
