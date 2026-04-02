package com.fundmanager.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeRequest(
        @NotBlank(message = "Fund code is required") String fundCode,
        String fundName,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
        @Positive(message = "Shares must be positive") BigDecimal shares,
        BigDecimal fee,
        @NotNull(message = "NAV is required") @Positive(message = "NAV must be positive") BigDecimal nav,
        LocalDate tradeDate,
        String remark
) {
}
