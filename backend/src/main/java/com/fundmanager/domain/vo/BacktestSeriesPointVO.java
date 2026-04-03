package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestSeriesPointVO(
        LocalDate date,
        BigDecimal nav,
        BigDecimal portfolioValue,
        BigDecimal investedAmount,
        BigDecimal cashBalance,
        BigDecimal shareBalance
) {
}
