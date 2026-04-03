package com.fundmanager.service;

import com.fundmanager.domain.entity.FundHoldingSnapshot;
import com.fundmanager.domain.entity.FundInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class EquityHoldingEstimateStrategyTest {

    private final SecurityQuoteClient securityQuoteClient = Mockito.mock(SecurityQuoteClient.class);
    private final EquityHoldingEstimateStrategy strategy = new EquityHoldingEstimateStrategy(securityQuoteClient);

    @Test
    void shouldEstimateEquityFundUsingHoldings() {
        when(securityQuoteClient.quotes(anyCollection())).thenReturn(Map.of(
                "600519", new SecurityQuoteSnapshot("600519", "贵州茅台", new BigDecimal("1500.00"), new BigDecimal("0.0500")),
                "000001", new SecurityQuoteSnapshot("000001", "平安银行", new BigDecimal("12.00"), new BigDecimal("-0.0200"))
        ));

        FundEstimateContext context = new FundEstimateContext(
                "161725",
                fundInfo("普通股票型"),
                new BigDecimal("1.000000"),
                List.of(
                        holding("600519", "60.00"),
                        holding("000001", "20.00")
                )
        );

        FundEstimateComputation result = strategy.estimate(context).orElseThrow();

        assertEquals("self_holdings", result.estimateSource());
        assertEquals("high", result.estimateConfidence());
        assertEquals(new BigDecimal("0.8000"), result.holdingCoverageRate());
        assertEquals(new BigDecimal("0.8000"), result.quotedCoverageRate());
        assertEquals(new BigDecimal("1.026000"), result.estimateNav());
        assertEquals(new BigDecimal("2.6000"), result.estimateGrowthRate());
        assertNotNull(result.estimateUpdatedAt());
    }

    @Test
    void shouldFallbackWhenHoldingCoverageTooLow() {
        FundEstimateContext context = new FundEstimateContext(
                "161725",
                fundInfo("偏股混合型"),
                new BigDecimal("1.000000"),
                List.of(holding("600519", "20.00"))
        );

        assertTrue(strategy.estimate(context).isEmpty());
    }

    @Test
    void shouldFallbackWhenQuotedCoverageTooLow() {
        when(securityQuoteClient.quotes(anyCollection())).thenReturn(Map.of(
                "600519", new SecurityQuoteSnapshot("600519", "贵州茅台", new BigDecimal("1500.00"), new BigDecimal("0.0500"))
        ));

        FundEstimateContext context = new FundEstimateContext(
                "161725",
                fundInfo("灵活配置型"),
                new BigDecimal("1.000000"),
                List.of(
                        holding("600519", "10.00"),
                        holding("000001", "30.00")
                )
        );

        assertTrue(strategy.estimate(context).isEmpty());
    }

    @Test
    void shouldNotSupportBondFund() {
        FundEstimateContext context = new FundEstimateContext(
                "000001",
                fundInfo("中长期纯债型"),
                new BigDecimal("1.000000"),
                List.of()
        );

        assertFalse(strategy.supports(context));
    }

    private FundInfo fundInfo(String type) {
        FundInfo info = new FundInfo();
        info.setFundCode("161725");
        info.setFundName("Fund A");
        info.setFundType(type);
        info.setStatus((byte) 1);
        info.setCreatedAt(LocalDateTime.now());
        info.setUpdatedAt(LocalDateTime.now());
        return info;
    }

    private FundHoldingSnapshot holding(String stockCode, String navRatio) {
        FundHoldingSnapshot snapshot = new FundHoldingSnapshot();
        snapshot.setFundCode("161725");
        snapshot.setStockCode(stockCode);
        snapshot.setStockName(stockCode);
        snapshot.setNavRatio(new BigDecimal(navRatio));
        snapshot.setCreatedAt(LocalDateTime.now());
        return snapshot;
    }
}
