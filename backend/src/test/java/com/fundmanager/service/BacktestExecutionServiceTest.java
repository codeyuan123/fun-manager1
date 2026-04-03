package com.fundmanager.service;

import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.BacktestResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BacktestExecutionServiceTest {

    private final BacktestStrategyEngine strategyEngine = new BacktestStrategyEngine();
    private final BacktestExecutionService executionService = new BacktestExecutionService(strategyEngine);

    @Test
    void shouldProduceDifferentResultForSameDayAndNextTradeDay() {
        List<FundNav> navs = navs(
                "2026-04-03", "1.00",
                "2026-04-07", "1.20",
                "2026-04-08", "1.10"
        );

        BacktestResultVO sameDay = executionService.execute(
                "161725",
                "Fund A",
                navs,
                BigDecimal.ZERO,
                "lump_sum",
                Map.of(),
                LocalDate.of(2026, 4, 7),
                LocalDate.of(2026, 4, 8),
                "same_day",
                new BigDecimal("10000")
        );

        BacktestResultVO nextTrade = executionService.execute(
                "161725",
                "Fund A",
                navs,
                BigDecimal.ZERO,
                "lump_sum",
                Map.of(),
                LocalDate.of(2026, 4, 7),
                LocalDate.of(2026, 4, 8),
                "next_trade_day",
                new BigDecimal("10000")
        );

        assertFalse(sameDay.series().isEmpty());
        assertFalse(nextTrade.series().isEmpty());
        assertEquals(new BigDecimal("9166.67"), sameDay.summary().endingValue());
        assertEquals(new BigDecimal("10000.00"), nextTrade.summary().endingValue());
    }

    @Test
    void shouldRunAllBuiltinStrategies() {
        List<FundNav> navs = navs(
                "2025-01-02", "1.00",
                "2025-01-03", "1.01",
                "2025-01-06", "1.02",
                "2025-01-07", "0.98",
                "2025-01-08", "0.96",
                "2025-01-09", "0.99",
                "2025-01-10", "1.03",
                "2025-02-10", "1.05",
                "2025-03-10", "0.95",
                "2025-04-10", "1.08",
                "2025-05-12", "1.10",
                "2025-06-10", "1.06",
                "2025-07-10", "1.12",
                "2025-08-11", "1.07",
                "2025-09-10", "1.15",
                "2025-10-10", "1.14",
                "2025-11-10", "1.18",
                "2025-12-10", "1.21"
        );

        for (BacktestStrategyEngine.BacktestStrategyDefinition definition : strategyEngine.builtinStrategies()) {
            BacktestResultVO result = executionService.execute(
                    "161725",
                    "Fund A",
                    navs,
                    new BigDecimal("0.10"),
                    definition.code(),
                    definition.defaults(),
                    LocalDate.of(2025, 1, 2),
                    LocalDate.of(2025, 12, 10),
                    "next_trade_day",
                    new BigDecimal("50000")
            );
            assertNotNull(result.summary());
            assertFalse(result.series().isEmpty());
        }
    }

    private List<FundNav> navs(String... data) {
        java.util.ArrayList<FundNav> result = new java.util.ArrayList<>();
        for (int i = 0; i < data.length; i += 2) {
            FundNav nav = new FundNav();
            nav.setFundCode("161725");
            nav.setNavDate(LocalDate.parse(data[i]));
            nav.setUnitNav(new BigDecimal(data[i + 1]));
            nav.setAccumulatedNav(new BigDecimal(data[i + 1]));
            nav.setCreatedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
            result.add(nav);
        }
        return result;
    }
}
