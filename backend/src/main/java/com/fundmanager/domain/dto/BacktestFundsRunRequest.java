package com.fundmanager.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BacktestFundsRunRequest(
        List<String> fundCodes,
        LocalDate startDate,
        LocalDate endDate,
        String executionMode,
        BigDecimal initialCapital,
        String strategyCode,
        Map<String, Object> strategyParams
) {
}
