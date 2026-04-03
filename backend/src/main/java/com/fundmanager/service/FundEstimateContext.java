package com.fundmanager.service;

import com.fundmanager.domain.entity.FundHoldingSnapshot;
import com.fundmanager.domain.entity.FundInfo;

import java.math.BigDecimal;
import java.util.List;

public record FundEstimateContext(
        String fundCode,
        FundInfo fundInfo,
        BigDecimal latestNav,
        List<FundHoldingSnapshot> holdings
) {
}
