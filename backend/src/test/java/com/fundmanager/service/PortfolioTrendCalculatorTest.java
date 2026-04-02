package com.fundmanager.service;

import com.fundmanager.domain.entity.FundTransaction;
import com.fundmanager.domain.vo.TrendPointVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioTrendCalculatorTest {

    private final PortfolioTrendCalculator calculator = new PortfolioTrendCalculator();

    @Test
    void shouldRebuildRealTrendFromTransactionsAndNav() {
        LocalDate start = LocalDate.of(2026, 3, 30);
        LocalDate end = LocalDate.of(2026, 4, 2);

        FundTransaction buy = tx("BUY", LocalDate.of(2026, 3, 30), "161725", "1000", "100");
        FundTransaction sell = tx("SELL", LocalDate.of(2026, 4, 1), "161725", "220", "20");

        NavigableMap<LocalDate, BigDecimal> navSeries = new TreeMap<>();
        navSeries.put(LocalDate.of(2026, 3, 30), new BigDecimal("10.00"));
        navSeries.put(LocalDate.of(2026, 3, 31), new BigDecimal("10.50"));
        navSeries.put(LocalDate.of(2026, 4, 1), new BigDecimal("11.00"));

        List<TrendPointVO> trend = calculator.calculate(
                start,
                end,
                List.of(buy, sell),
                Map.of("161725", navSeries),
                Map.of("161725", new BigDecimal("12.50"))
        );

        assertEquals(4, trend.size());
        assertEquals(new BigDecimal("0.00"), trend.get(0).profit());
        assertEquals(new BigDecimal("50.00"), trend.get(1).profit());
        assertEquals(new BigDecimal("80.00"), trend.get(2).profit());
        assertEquals(new BigDecimal("200.00"), trend.get(3).profit());
    }

    private FundTransaction tx(String type, LocalDate tradeDate, String fundCode, String amount, String shares) {
        FundTransaction transaction = new FundTransaction();
        transaction.setTransactionType(type);
        transaction.setTradeDate(tradeDate);
        transaction.setFundCode(fundCode);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setShares(new BigDecimal(shares));
        transaction.setFee(BigDecimal.ZERO);
        transaction.setNav(new BigDecimal("10.00"));
        transaction.setCreatedAt(LocalDateTime.of(2026, 4, 2, 10, 0));
        return transaction;
    }
}
