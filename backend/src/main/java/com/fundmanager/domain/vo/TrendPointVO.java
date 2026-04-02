package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointVO(
        LocalDate date,
        BigDecimal profit
) {
}
