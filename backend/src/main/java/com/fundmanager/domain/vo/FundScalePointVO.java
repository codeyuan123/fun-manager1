package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record FundScalePointVO(
        String date,
        BigDecimal value,
        String mom
) {
}
