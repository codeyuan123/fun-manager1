package com.fundmanager.service;

import com.fundmanager.domain.vo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {

    private final CurrentUserService currentUserService;
    private final PositionService positionService;

    public DashboardService(CurrentUserService currentUserService, PositionService positionService) {
        this.currentUserService = currentUserService;
        this.positionService = positionService;
    }

    public DashboardOverviewVO overview(String username) {
        List<PositionItemVO> positions = loadPositions(username);
        BigDecimal totalCost = sum(positions.stream().map(PositionItemVO::currentCost).toList());
        BigDecimal totalMarketValue = sum(positions.stream().map(PositionItemVO::marketValue).toList());
        BigDecimal totalProfit = totalMarketValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProfitRate = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalProfit.divide(totalCost, 4, RoundingMode.HALF_UP);
        BigDecimal totalTodayProfit = sum(positions.stream().map(PositionItemVO::todayProfit).toList());
        return new DashboardOverviewVO(
                totalCost,
                totalMarketValue,
                totalProfit,
                totalProfitRate,
                totalTodayProfit,
                positions.size()
        );
    }

    public List<TrendPointVO> trend(String username) {
        List<PositionItemVO> positions = loadPositions(username);
        BigDecimal currentProfit = sum(positions.stream().map(PositionItemVO::estimatedProfit).toList());
        List<TrendPointVO> points = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            BigDecimal factor = BigDecimal.valueOf(7 - i).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);
            points.add(new TrendPointVO(day, currentProfit.multiply(factor).setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }

    public List<DistributionItemVO> distribution(String username) {
        List<PositionItemVO> positions = loadPositions(username);
        Map<String, BigDecimal> costMap = new HashMap<>();
        Map<String, BigDecimal> marketMap = new HashMap<>();
        for (PositionItemVO item : positions) {
            String key = item.fundType() == null ? "UNKNOWN" : item.fundType();
            costMap.put(key, costMap.getOrDefault(key, BigDecimal.ZERO).add(item.currentCost()));
            marketMap.put(key, marketMap.getOrDefault(key, BigDecimal.ZERO).add(item.marketValue()));
        }
        return costMap.keySet().stream()
                .sorted()
                .map(key -> new DistributionItemVO(
                        key,
                        costMap.getOrDefault(key, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                        marketMap.getOrDefault(key, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public Map<String, List<RankingItemVO>> ranking(String username) {
        List<PositionItemVO> positions = loadPositions(username);
        List<RankingItemVO> all = positions.stream()
                .map(p -> new RankingItemVO(p.fundCode(), p.fundName(), p.estimatedProfit(), p.estimatedProfitRate()))
                .toList();
        List<RankingItemVO> profitTop = all.stream()
                .sorted(Comparator.comparing(RankingItemVO::estimatedProfit).reversed())
                .limit(5)
                .toList();
        List<RankingItemVO> lossTop = all.stream()
                .sorted(Comparator.comparing(RankingItemVO::estimatedProfit))
                .limit(5)
                .toList();
        return Map.of("profitTop", profitTop, "lossTop", lossTop);
    }

    private List<PositionItemVO> loadPositions(String username) {
        Long userId = currentUserService.getUserId(username);
        return positionService.listByUserId(userId);
    }

    private BigDecimal sum(List<BigDecimal> values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            result = result.add(value == null ? BigDecimal.ZERO : value);
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }
}
