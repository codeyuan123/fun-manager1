package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BacktestStrategyEngine {

    public record BacktestStrategyDefinition(
            String code,
            String name,
            Map<String, Object> defaults
    ) {
    }

    public BacktestStrategyDefinition definition(String code) {
        return switch (normalize(code)) {
            case "lump_sum" -> new BacktestStrategyDefinition("lump_sum", "一次性买入", Map.of());
            case "dca_daily" -> new BacktestStrategyDefinition("dca_daily", "日定投", Map.of(
                    "periodicAmount", new BigDecimal("100")
            ));
            case "dca_weekly" -> new BacktestStrategyDefinition("dca_weekly", "周定投", Map.of(
                    "periodicAmount", new BigDecimal("500"),
                    "weekday", 1
            ));
            case "dca_monthly" -> new BacktestStrategyDefinition("dca_monthly", "月定投", Map.of(
                    "periodicAmount", new BigDecimal("1000"),
                    "dayOfMonth", 8
            ));
            case "drawdown_add" -> new BacktestStrategyDefinition("drawdown_add", "回撤加仓", Map.of(
                    "baseAmount", new BigDecimal("1000"),
                    "extraAmount", new BigDecimal("2000"),
                    "drawdownThreshold", new BigDecimal("0.10"),
                    "drawdownWindowDays", 60,
                    "dayOfMonth", 8
            ));
            case "ma_timing" -> new BacktestStrategyDefinition("ma_timing", "均线择时", Map.of(
                    "maPeriod", 20
            ));
            case "nav_percentile_dca" -> new BacktestStrategyDefinition("nav_percentile_dca", "净值分位定投", Map.of(
                    "baseAmount", new BigDecimal("1000"),
                    "windowDays", 250,
                    "mediumPercentile", new BigDecimal("0.30"),
                    "deepPercentile", new BigDecimal("0.15"),
                    "mediumMultiplier", new BigDecimal("2"),
                    "deepMultiplier", new BigDecimal("3"),
                    "dayOfMonth", 8
            ));
            case "grid_add" -> new BacktestStrategyDefinition("grid_add", "网格加仓", Map.of(
                    "baseAmount", new BigDecimal("10000"),
                    "gridStep", new BigDecimal("0.05"),
                    "gridAmount", new BigDecimal("2000"),
                    "maxGrids", 5
            ));
            default -> throw new BusinessException("Unsupported strategy: " + code);
        };
    }

    public void validateStrategyCodes(Collection<String> strategyCodes) {
        if (strategyCodes == null || strategyCodes.isEmpty()) {
            throw new BusinessException("At least one strategy is required");
        }
        for (String strategyCode : strategyCodes) {
            definition(strategyCode);
        }
    }

    public Map<String, Object> mergeParams(String code, Map<String, Object> overrides) {
        BacktestStrategyDefinition definition = definition(code);
        Map<String, Object> params = new LinkedHashMap<>(definition.defaults());
        if (overrides != null) {
            params.putAll(overrides);
        }
        return params;
    }

    public List<BacktestStrategyDefinition> builtinStrategies() {
        return List.of(
                definition("lump_sum"),
                definition("dca_daily"),
                definition("dca_weekly"),
                definition("dca_monthly"),
                definition("drawdown_add"),
                definition("ma_timing"),
                definition("nav_percentile_dca"),
                definition("grid_add")
        );
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toLowerCase();
    }
}
