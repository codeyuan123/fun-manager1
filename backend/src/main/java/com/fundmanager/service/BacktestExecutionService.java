package com.fundmanager.service;

import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.BacktestResultVO;
import com.fundmanager.domain.vo.BacktestSeriesPointVO;
import com.fundmanager.domain.vo.BacktestSummaryVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class BacktestExecutionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final BacktestStrategyEngine strategyEngine;

    public BacktestExecutionService(BacktestStrategyEngine strategyEngine) {
        this.strategyEngine = strategyEngine;
    }

    public BacktestResultVO execute(String fundCode,
                                    String fundName,
                                    List<FundNav> allNavs,
                                    BigDecimal feeRatePercent,
                                    String strategyCode,
                                    Map<String, Object> rawParams,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String executionMode,
                                    BigDecimal initialCapital) {
        List<FundNav> sortedNavs = allNavs.stream()
                .sorted(Comparator.comparing(FundNav::getNavDate))
                .toList();
        List<FundNav> tradeSeries = sortedNavs.stream()
                .filter(item -> !item.getNavDate().isBefore(startDate) && !item.getNavDate().isAfter(endDate))
                .toList();

        NavigableSet<LocalDate> tradeDates = new TreeSet<>();
        Map<LocalDate, Integer> indexByDate = new HashMap<>();
        for (int i = 0; i < sortedNavs.size(); i++) {
            LocalDate date = sortedNavs.get(i).getNavDate();
            indexByDate.put(date, i);
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                tradeDates.add(date);
            }
        }

        SimulationState state = new SimulationState(initialCapital == null ? ZERO : initialCapital.max(ZERO));
        Map<LocalDate, List<TradeCommand>> scheduledCommands = new HashMap<>();
        Map<String, Object> params = strategyEngine.mergeParams(strategyCode, rawParams);
        String normalizedExecutionMode = normalizeExecutionMode(executionMode);
        String normalizedStrategy = strategyCode == null ? "" : strategyCode.trim().toLowerCase();
        BigDecimal feeRateRatio = nvl(feeRatePercent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        LocalDate baseExecutionDate = resolveExecutionDate(startDate, normalizedExecutionMode, tradeDates);
        if ("lump_sum".equals(normalizedStrategy) && baseExecutionDate != null) {
            addCommand(scheduledCommands, baseExecutionDate, TradeCommand.buyAmount(state.cash));
        } else if ("dca_daily".equals(normalizedStrategy)) {
            scheduleDaily(startDate, endDate, decimalParam(params, "periodicAmount"), normalizedExecutionMode, tradeDates, scheduledCommands);
        } else if ("dca_weekly".equals(normalizedStrategy)) {
            scheduleWeekly(startDate, endDate, intParam(params, "weekday"), decimalParam(params, "periodicAmount"), normalizedExecutionMode, tradeDates, scheduledCommands);
        } else if ("dca_monthly".equals(normalizedStrategy)) {
            scheduleMonthly(startDate, endDate, intParam(params, "dayOfMonth"), decimalParam(params, "periodicAmount"), normalizedExecutionMode, tradeDates, scheduledCommands);
        } else if ("grid_add".equals(normalizedStrategy) && baseExecutionDate != null) {
            addCommand(scheduledCommands, baseExecutionDate, TradeCommand.buyAmount(decimalParam(params, "baseAmount")));
        }

        BigDecimal gridAnchorNav = null;
        int gridsUsed = 0;
        List<BacktestSeriesPointVO> series = new ArrayList<>();

        for (FundNav navPoint : tradeSeries) {
            LocalDate currentDate = navPoint.getNavDate();
            BigDecimal currentNav = nvl(navPoint.getUnitNav());

            List<TradeCommand> commands = scheduledCommands.remove(currentDate);
            if (commands != null) {
                for (TradeCommand command : commands) {
                    applyCommand(state, command, currentNav, feeRateRatio);
                    if ("grid_add".equals(normalizedStrategy) && gridAnchorNav == null && command.isBuyCommand()) {
                        gridAnchorNav = currentNav;
                    }
                }
            }

            int currentIndex = indexByDate.getOrDefault(currentDate, -1);
            if ("drawdown_add".equals(normalizedStrategy)
                    && isMonthlyExecutionDate(currentDate, startDate, endDate, intParam(params, "dayOfMonth"), normalizedExecutionMode, tradeDates)) {
                applyCommand(state, TradeCommand.buyAmount(decimalParam(params, "baseAmount")), currentNav, feeRateRatio);
                BigDecimal drawdown = drawdownRate(sortedNavs, currentIndex, intParam(params, "drawdownWindowDays"), currentNav);
                if (drawdown.compareTo(decimalParam(params, "drawdownThreshold")) >= 0) {
                    applyCommand(state, TradeCommand.buyAmount(decimalParam(params, "extraAmount")), currentNav, feeRateRatio);
                }
            }

            if ("nav_percentile_dca".equals(normalizedStrategy)
                    && isMonthlyExecutionDate(currentDate, startDate, endDate, intParam(params, "dayOfMonth"), normalizedExecutionMode, tradeDates)) {
                BigDecimal percentile = navPercentile(sortedNavs, currentIndex, intParam(params, "windowDays"), currentNav);
                BigDecimal baseAmount = decimalParam(params, "baseAmount");
                BigDecimal multiplier = ONE;
                if (percentile.compareTo(decimalParam(params, "deepPercentile")) <= 0) {
                    multiplier = decimalParam(params, "deepMultiplier");
                } else if (percentile.compareTo(decimalParam(params, "mediumPercentile")) <= 0) {
                    multiplier = decimalParam(params, "mediumMultiplier");
                }
                applyCommand(state, TradeCommand.buyAmount(baseAmount.multiply(multiplier, MC)), currentNav, feeRateRatio);
            }

            if ("grid_add".equals(normalizedStrategy) && gridAnchorNav != null) {
                BigDecimal gridStep = decimalParam(params, "gridStep");
                BigDecimal gridAmount = decimalParam(params, "gridAmount");
                int maxGrids = intParam(params, "maxGrids");
                while (gridsUsed < maxGrids
                        && currentNav.compareTo(gridAnchorNav.multiply(ONE.subtract(gridStep, MC), MC)) <= 0) {
                    LocalDate executeDate = resolveExecutionDate(currentDate, normalizedExecutionMode, tradeDates);
                    if (executeDate == null) {
                        break;
                    }
                    if (executeDate.equals(currentDate)) {
                        applyCommand(state, TradeCommand.buyAmount(gridAmount), currentNav, feeRateRatio);
                    } else {
                        addCommand(scheduledCommands, executeDate, TradeCommand.buyAmount(gridAmount));
                    }
                    gridsUsed++;
                    gridAnchorNav = gridAnchorNav.multiply(ONE.subtract(gridStep, MC), MC);
                }
            }

            if ("ma_timing".equals(normalizedStrategy) && currentIndex > 0) {
                int maPeriod = intParam(params, "maPeriod");
                BigDecimal previousMa = movingAverage(sortedNavs, currentIndex - 1, maPeriod);
                BigDecimal currentMa = movingAverage(sortedNavs, currentIndex, maPeriod);
                if (previousMa != null && currentMa != null) {
                    BigDecimal previousNav = nvl(sortedNavs.get(currentIndex - 1).getUnitNav());
                    if (previousNav.compareTo(previousMa) <= 0 && currentNav.compareTo(currentMa) > 0) {
                        LocalDate executeDate = resolveExecutionDate(currentDate, normalizedExecutionMode, tradeDates);
                        if (executeDate != null) {
                            if (executeDate.equals(currentDate)) {
                                applyCommand(state, TradeCommand.buyAllCash(), currentNav, feeRateRatio);
                            } else {
                                addCommand(scheduledCommands, executeDate, TradeCommand.buyAllCash());
                            }
                        }
                    } else if (previousNav.compareTo(previousMa) >= 0 && currentNav.compareTo(currentMa) < 0) {
                        LocalDate executeDate = resolveExecutionDate(currentDate, normalizedExecutionMode, tradeDates);
                        if (executeDate != null) {
                            if (executeDate.equals(currentDate)) {
                                applyCommand(state, TradeCommand.sellAll(), currentNav, feeRateRatio);
                            } else {
                                addCommand(scheduledCommands, executeDate, TradeCommand.sellAll());
                            }
                        }
                    }
                }
            }

            BigDecimal portfolioValue = state.cash.add(state.shares.multiply(currentNav, MC));
            series.add(new BacktestSeriesPointVO(
                    currentDate,
                    currentNav.setScale(6, RoundingMode.HALF_UP),
                    portfolioValue.setScale(2, RoundingMode.HALF_UP),
                    state.totalInvested.setScale(2, RoundingMode.HALF_UP),
                    state.cash.setScale(2, RoundingMode.HALF_UP),
                    state.shares.setScale(4, RoundingMode.HALF_UP)
            ));
        }

        return new BacktestResultVO(
                fundCode,
                fundName,
                normalizedStrategy,
                strategyEngine.definition(normalizedStrategy).name(),
                summarize(series),
                series
        );
    }

    private void scheduleDaily(LocalDate start,
                               LocalDate end,
                               BigDecimal amount,
                               String executionMode,
                               NavigableSet<LocalDate> tradeDates,
                               Map<LocalDate, List<TradeCommand>> commands) {
        for (LocalDate current = start; !current.isAfter(end); current = current.plusDays(1)) {
            addBuy(commands, resolveExecutionDate(current, executionMode, tradeDates), amount);
        }
    }

    private void scheduleWeekly(LocalDate start,
                                LocalDate end,
                                int weekday,
                                BigDecimal amount,
                                String executionMode,
                                NavigableSet<LocalDate> tradeDates,
                                Map<LocalDate, List<TradeCommand>> commands) {
        DayOfWeek target = DayOfWeek.of(Math.max(1, Math.min(7, weekday)));
        for (LocalDate current = start; !current.isAfter(end); current = current.plusDays(1)) {
            if (current.getDayOfWeek() == target) {
                addBuy(commands, resolveExecutionDate(current, executionMode, tradeDates), amount);
            }
        }
    }

    private void scheduleMonthly(LocalDate start,
                                 LocalDate end,
                                 int dayOfMonth,
                                 BigDecimal amount,
                                 String executionMode,
                                 NavigableSet<LocalDate> tradeDates,
                                 Map<LocalDate, List<TradeCommand>> commands) {
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            LocalDate scheduled = cursor.withDayOfMonth(Math.min(dayOfMonth, cursor.lengthOfMonth()));
            if (!scheduled.isBefore(start) && !scheduled.isAfter(end)) {
                addBuy(commands, resolveExecutionDate(scheduled, executionMode, tradeDates), amount);
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
    }

    private boolean isMonthlyExecutionDate(LocalDate currentDate,
                                           LocalDate start,
                                           LocalDate end,
                                           int dayOfMonth,
                                           String executionMode,
                                           NavigableSet<LocalDate> tradeDates) {
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            LocalDate scheduled = cursor.withDayOfMonth(Math.min(dayOfMonth, cursor.lengthOfMonth()));
            if (currentDate.equals(resolveExecutionDate(scheduled, executionMode, tradeDates))) {
                return true;
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
        return false;
    }

    private void addBuy(Map<LocalDate, List<TradeCommand>> commands, LocalDate date, BigDecimal amount) {
        if (date == null || amount == null || amount.compareTo(ZERO) <= 0) {
            return;
        }
        addCommand(commands, date, TradeCommand.buyAmount(amount));
    }

    private void addCommand(Map<LocalDate, List<TradeCommand>> commands, LocalDate date, TradeCommand command) {
        if (date == null) {
            return;
        }
        commands.computeIfAbsent(date, ignored -> new ArrayList<>()).add(command);
    }

    private LocalDate resolveExecutionDate(LocalDate signalDate, String executionMode, NavigableSet<LocalDate> tradeDates) {
        if (tradeDates.isEmpty()) {
            return null;
        }
        if ("same_day".equals(executionMode)) {
            return tradeDates.ceiling(signalDate);
        }
        return tradeDates.contains(signalDate) ? tradeDates.higher(signalDate) : tradeDates.ceiling(signalDate);
    }

    private void applyCommand(SimulationState state, TradeCommand command, BigDecimal nav, BigDecimal feeRateRatio) {
        if (nav == null || nav.compareTo(ZERO) <= 0) {
            return;
        }
        switch (command.type()) {
            case BUY_AMOUNT -> buy(state, command.amount(), nav, feeRateRatio);
            case BUY_ALL_CASH -> buy(state, state.cash, nav, feeRateRatio);
            case SELL_ALL -> sellAll(state, nav);
        }
    }

    private void buy(SimulationState state, BigDecimal desiredAmount, BigDecimal nav, BigDecimal feeRateRatio) {
        if (state.cash.compareTo(ZERO) <= 0 || desiredAmount == null || desiredAmount.compareTo(ZERO) <= 0) {
            return;
        }
        BigDecimal grossAmount = desiredAmount.min(state.cash.divide(ONE.add(feeRateRatio, MC), 8, RoundingMode.HALF_UP));
        if (grossAmount.compareTo(ZERO) <= 0) {
            return;
        }
        BigDecimal fee = grossAmount.multiply(feeRateRatio, MC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shares = grossAmount.divide(nav, 8, RoundingMode.HALF_UP);
        state.cash = state.cash.subtract(grossAmount.add(fee));
        if (state.cash.compareTo(ZERO) < 0) {
            state.cash = ZERO;
        }
        state.shares = state.shares.add(shares);
        state.totalInvested = state.totalInvested.add(grossAmount).add(fee);
    }

    private void sellAll(SimulationState state, BigDecimal nav) {
        if (state.shares.compareTo(ZERO) <= 0) {
            return;
        }
        state.cash = state.cash.add(state.shares.multiply(nav, MC));
        state.shares = ZERO;
    }

    private BigDecimal movingAverage(List<FundNav> navs, int currentIndex, int period) {
        if (period <= 0 || currentIndex < period - 1) {
            return null;
        }
        BigDecimal total = ZERO;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            total = total.add(nvl(navs.get(i).getUnitNav()));
        }
        return total.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal drawdownRate(List<FundNav> navs, int currentIndex, int windowDays, BigDecimal currentNav) {
        if (currentIndex < 0) {
            return ZERO;
        }
        int startIndex = Math.max(0, currentIndex - windowDays + 1);
        BigDecimal peak = currentNav;
        for (int i = startIndex; i <= currentIndex; i++) {
            peak = peak.max(nvl(navs.get(i).getUnitNav()));
        }
        if (peak.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return peak.subtract(currentNav).divide(peak, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal navPercentile(List<FundNav> navs, int currentIndex, int windowDays, BigDecimal currentNav) {
        if (currentIndex < 0) {
            return ONE;
        }
        int startIndex = Math.max(0, currentIndex - windowDays + 1);
        int total = 0;
        int lowerOrEqual = 0;
        for (int i = startIndex; i <= currentIndex; i++) {
            BigDecimal value = nvl(navs.get(i).getUnitNav());
            total++;
            if (value.compareTo(currentNav) <= 0) {
                lowerOrEqual++;
            }
        }
        if (total == 0) {
            return ONE;
        }
        return BigDecimal.valueOf(lowerOrEqual).divide(BigDecimal.valueOf(total), 8, RoundingMode.HALF_UP);
    }

    private BacktestSummaryVO summarize(List<BacktestSeriesPointVO> series) {
        if (series.isEmpty()) {
            return new BacktestSummaryVO(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, 0, ZERO, ZERO);
        }

        BigDecimal totalInvested = series.get(series.size() - 1).investedAmount();
        BigDecimal endingValue = series.get(series.size() - 1).portfolioValue();
        BigDecimal cumulativeReturnRate = totalInvested.compareTo(ZERO) <= 0
                ? ZERO
                : endingValue.subtract(totalInvested).divide(totalInvested, 8, RoundingMode.HALF_UP);

        List<BigDecimal> dailyReturns = new ArrayList<>();
        for (int i = 1; i < series.size(); i++) {
            BigDecimal previous = series.get(i - 1).portfolioValue();
            BigDecimal current = series.get(i).portfolioValue();
            if (previous.compareTo(ZERO) > 0) {
                dailyReturns.add(current.subtract(previous).divide(previous, 8, RoundingMode.HALF_UP));
            }
        }

        BigDecimal annualizedReturnRate = ZERO;
        if (totalInvested.compareTo(ZERO) > 0 && series.size() > 1) {
            double years = Math.max(1d / 365d, (series.size() - 1) / 252d);
            double ratio = endingValue.divide(totalInvested, 8, RoundingMode.HALF_UP).doubleValue();
            annualizedReturnRate = ratio <= 0 ? ZERO : BigDecimal.valueOf(Math.pow(ratio, 1d / years) - 1d);
        }

        BigDecimal maxDrawdownRate = ZERO;
        BigDecimal peak = series.get(0).portfolioValue();
        int peakIndex = 0;
        int recoveryDays = 0;
        Integer pendingRecoveryStart = null;

        for (int i = 0; i < series.size(); i++) {
            BigDecimal value = series.get(i).portfolioValue();
            if (value.compareTo(peak) >= 0) {
                peak = value;
                peakIndex = i;
                if (pendingRecoveryStart != null) {
                    recoveryDays = Math.max(recoveryDays, i - pendingRecoveryStart);
                    pendingRecoveryStart = null;
                }
                continue;
            }

            if (peak.compareTo(ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(value).divide(peak, 8, RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdownRate) > 0) {
                    maxDrawdownRate = drawdown;
                    pendingRecoveryStart = peakIndex;
                }
            }
        }

        if (pendingRecoveryStart != null) {
            recoveryDays = Math.max(recoveryDays, series.size() - 1 - pendingRecoveryStart);
        }

        BigDecimal annualizedVolatility = stddev(dailyReturns).multiply(BigDecimal.valueOf(Math.sqrt(252d)), MC);
        BigDecimal downsideDeviation = stddev(
                dailyReturns.stream().filter(value -> value.compareTo(ZERO) < 0).map(BigDecimal::abs).toList()
        ).multiply(BigDecimal.valueOf(Math.sqrt(252d)), MC);
        BigDecimal meanReturn = average(dailyReturns);
        BigDecimal sharpeRatio = annualizedVolatility.compareTo(ZERO) == 0
                ? ZERO
                : meanReturn.multiply(BigDecimal.valueOf(Math.sqrt(252d)), MC)
                .divide(annualizedVolatility, 8, RoundingMode.HALF_UP);
        BigDecimal sortinoRatio = downsideDeviation.compareTo(ZERO) == 0
                ? ZERO
                : meanReturn.multiply(BigDecimal.valueOf(Math.sqrt(252d)), MC)
                .divide(downsideDeviation, 8, RoundingMode.HALF_UP);
        BigDecimal calmarRatio = maxDrawdownRate.compareTo(ZERO) == 0
                ? ZERO
                : annualizedReturnRate.divide(maxDrawdownRate, 8, RoundingMode.HALF_UP);
        long positiveDays = dailyReturns.stream().filter(value -> value.compareTo(ZERO) > 0).count();
        BigDecimal winRate = dailyReturns.isEmpty()
                ? ZERO
                : BigDecimal.valueOf(positiveDays).divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);

        return new BacktestSummaryVO(
                cumulativeReturnRate.setScale(4, RoundingMode.HALF_UP),
                annualizedReturnRate.setScale(4, RoundingMode.HALF_UP),
                maxDrawdownRate.setScale(4, RoundingMode.HALF_UP),
                annualizedVolatility.setScale(4, RoundingMode.HALF_UP),
                sharpeRatio.setScale(4, RoundingMode.HALF_UP),
                calmarRatio.setScale(4, RoundingMode.HALF_UP),
                sortinoRatio.setScale(4, RoundingMode.HALF_UP),
                winRate.setScale(4, RoundingMode.HALF_UP),
                recoveryDays,
                totalInvested.setScale(2, RoundingMode.HALF_UP),
                endingValue.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        BigDecimal total = ZERO;
        for (BigDecimal value : values) {
            total = total.add(nvl(value));
        }
        return total.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal stddev(List<BigDecimal> values) {
        if (values == null || values.size() < 2) {
            return ZERO;
        }
        BigDecimal avg = average(values);
        BigDecimal total = ZERO;
        for (BigDecimal value : values) {
            BigDecimal delta = nvl(value).subtract(avg);
            total = total.add(delta.multiply(delta, MC));
        }
        double variance = total.divide(BigDecimal.valueOf(values.size()), 12, RoundingMode.HALF_UP).doubleValue();
        return BigDecimal.valueOf(Math.sqrt(Math.max(variance, 0d)));
    }

    private BigDecimal decimalParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return value == null ? ZERO : new BigDecimal(String.valueOf(value));
    }

    private int intParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    private String normalizeExecutionMode(String executionMode) {
        return "same_day".equalsIgnoreCase(executionMode) ? "same_day" : "next_trade_day";
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private enum TradeActionType {
        BUY_AMOUNT,
        BUY_ALL_CASH,
        SELL_ALL
    }

    private record TradeCommand(TradeActionType type, BigDecimal amount) {
        private static TradeCommand buyAmount(BigDecimal amount) {
            return new TradeCommand(TradeActionType.BUY_AMOUNT, amount);
        }

        private static TradeCommand buyAllCash() {
            return new TradeCommand(TradeActionType.BUY_ALL_CASH, null);
        }

        private static TradeCommand sellAll() {
            return new TradeCommand(TradeActionType.SELL_ALL, null);
        }

        private boolean isBuyCommand() {
            return type == TradeActionType.BUY_AMOUNT || type == TradeActionType.BUY_ALL_CASH;
        }
    }

    private static class SimulationState {
        private BigDecimal cash;
        private BigDecimal shares = ZERO;
        private BigDecimal totalInvested = ZERO;

        private SimulationState(BigDecimal initialCapital) {
            this.cash = initialCapital;
        }
    }
}
