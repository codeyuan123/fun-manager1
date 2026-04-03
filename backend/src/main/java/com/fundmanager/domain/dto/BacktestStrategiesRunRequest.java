package com.fundmanager.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BacktestStrategiesRunRequest(
        String fundCode,
        LocalDate startDate,
        LocalDate endDate,
        String executionMode,
        BigDecimal initialCapital,
        List<String> strategyCodes,
        Map<String, Map<String, Object>> strategyParams
) {
}
