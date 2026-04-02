package com.fundmanager.domain.vo;

import java.math.BigDecimal;

public record FundReturnStatVO(
        String label,
        BigDecimal value
) {
}
