package com.fundmanager.service;

import com.fundmanager.domain.entity.FundTransaction;
import com.fundmanager.domain.vo.TrendPointVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
public class PortfolioTrendCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public List<TrendPointVO> calculate(LocalDate startDate,
                                        LocalDate endDate,
                                        List<FundTransaction> transactions,
                                        Map<String, NavigableMap<LocalDate, BigDecimal>> navHistory,
                                        Map<String, BigDecimal> latestPrices) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return List.of();
        }

        Map<String, List<FundTransaction>> transactionsByFund = new HashMap<>();
        for (FundTransaction transaction : transactions) {
            transactionsByFund.computeIfAbsent(transaction.getFundCode(), ignored -> new ArrayList<>())
                    .add(transaction);
        }

        Map<String, Integer> offsets = new HashMap<>();
        Map<String, PositionState> states = new HashMap<>();
        List<TrendPointVO> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (Map.Entry<String, List<FundTransaction>> entry : transactionsByFund.entrySet()) {
                String fundCode = entry.getKey();
                List<FundTransaction> fundTransactions = entry.getValue();
                int offset = offsets.getOrDefault(fundCode, 0);
                PositionState state = states.computeIfAbsent(fundCode, ignored -> new PositionState());
                while (offset < fundTransactions.size() && !fundTransactions.get(offset).getTradeDate().isAfter(date)) {
                    applyTransaction(state, fundTransactions.get(offset));
                    offset++;
                }
                offsets.put(fundCode, offset);
            }

            BigDecimal totalCost = ZERO;
            BigDecimal totalMarketValue = ZERO;
            for (Map.Entry<String, PositionState> entry : states.entrySet()) {
                PositionState state = entry.getValue();
                if (state.shares.compareTo(ZERO) <= 0) {
                    continue;
                }
                totalCost = totalCost.add(state.cost);
                BigDecimal nav = resolveNav(entry.getKey(), date, endDate, navHistory, latestPrices);
                totalMarketValue = totalMarketValue.add(state.shares.multiply(nav));
            }

            result.add(new TrendPointVO(
                    date,
                    totalMarketValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return result;
    }

    private void applyTransaction(PositionState state, FundTransaction transaction) {
        BigDecimal amount = nvl(transaction.getAmount());
        BigDecimal shares = nvl(transaction.getShares());
        BigDecimal fee = nvl(transaction.getFee());

        if ("BUY".equalsIgnoreCase(transaction.getTransactionType())) {
            state.shares = state.shares.add(shares);
            state.cost = state.cost.add(amount).add(fee);
            return;
        }

        if ("SELL".equalsIgnoreCase(transaction.getTransactionType()) && state.shares.compareTo(ZERO) > 0) {
            BigDecimal normalizedShares = shares.min(state.shares);
            BigDecimal avgCostPerShare = state.cost.divide(state.shares, 10, RoundingMode.HALF_UP);
            BigDecimal costReduction = avgCostPerShare.multiply(normalizedShares).setScale(2, RoundingMode.HALF_UP);
            state.shares = state.shares.subtract(normalizedShares);
            state.cost = state.cost.subtract(costReduction);
            if (state.cost.compareTo(ZERO) < 0) {
                state.cost = ZERO;
            }
        }
    }

    private BigDecimal resolveNav(String fundCode,
                                  LocalDate date,
                                  LocalDate endDate,
                                  Map<String, NavigableMap<LocalDate, BigDecimal>> navHistory,
                                  Map<String, BigDecimal> latestPrices) {
        if (date.equals(endDate) && latestPrices.containsKey(fundCode) && latestPrices.get(fundCode) != null) {
            return latestPrices.get(fundCode);
        }

        NavigableMap<LocalDate, BigDecimal> history = navHistory.getOrDefault(fundCode, new TreeMap<>());
        Map.Entry<LocalDate, BigDecimal> floor = history.floorEntry(date);
        if (floor != null && floor.getValue() != null) {
            return floor.getValue();
        }

        return latestPrices.getOrDefault(fundCode, ZERO);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static class PositionState {
        private BigDecimal shares = ZERO;
        private BigDecimal cost = ZERO;
    }
}
