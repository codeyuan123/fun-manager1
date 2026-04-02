package com.fundmanager.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionVO(
        Long id,
        String transactionType,
        LocalDate tradeDate,
        BigDecimal amount,
        BigDecimal shares,
        BigDecimal fee,
        BigDecimal nav,
        String remark,
        LocalDateTime createdAt
) {
}
