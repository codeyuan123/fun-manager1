package com.fundmanager.service;

import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.entity.FundTransaction;
import com.fundmanager.domain.vo.DashboardOverviewVO;
import com.fundmanager.domain.vo.DistributionItemVO;
import com.fundmanager.domain.vo.PositionItemVO;
import com.fundmanager.domain.vo.RankingItemVO;
import com.fundmanager.domain.vo.TrendPointVO;
import com.fundmanager.repository.FundNavRepository;
import com.fundmanager.repository.FundTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CurrentUserService currentUserService;
    private final PositionService positionService;
    private final FundTransactionRepository transactionRepository;
    private final FundNavRepository navRepository;
    private final FundQuoteService quoteService;
    private final PortfolioTrendCalculator portfolioTrendCalculator;

    public DashboardService(CurrentUserService currentUserService,
                            PositionService positionService,
                            FundTransactionRepository transactionRepository,
                            FundNavRepository navRepository,
                            FundQuoteService quoteService,
                            PortfolioTrendCalculator portfolioTrendCalculator) {
        this.currentUserService = currentUserService;
        this.positionService = positionService;
        this.transactionRepository = transactionRepository;
        this.navRepository = navRepository;
        this.quoteService = quoteService;
        this.portfolioTrendCalculator = portfolioTrendCalculator;
    }

    public DashboardOverviewVO overview(String username) {
        List<PositionItemVO> positions = loadPositions(username);
        BigDecimal totalCost = sum(positions.stream().map(PositionItemVO::currentCost).toList());
        BigDecimal totalMarketValue = sum(positions.stream().map(PositionItemVO::marketValue).toList());
        BigDecimal totalProfit = totalMarketValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProfitRate = totalCost.compareTo(ZERO) == 0
                ? ZERO
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
        Long userId = currentUserService.getUserId(username);
        List<FundTransaction> transactions = transactionRepository.findByUserIdOrderByTradeDateAscIdAsc(userId);
        if (transactions.isEmpty()) {
            return List.of();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        Map<String, NavigableMap<LocalDate, BigDecimal>> navHistory = new HashMap<>();
        Map<String, BigDecimal> latestPrices = new HashMap<>();

        transactions.stream()
                .map(FundTransaction::getFundCode)
                .distinct()
                .forEach(fundCode -> {
                    FundQuoteSnapshot snapshot = quoteService.loadSnapshot(fundCode, false);
                    latestPrices.put(fundCode, snapshot.currentNav());

                    TreeMap<LocalDate, BigDecimal> series = new TreeMap<>();
                    for (FundNav nav : navRepository.findByFundCodeOrderByNavDateAsc(fundCode)) {
                        series.put(nav.getNavDate(), nav.getUnitNav());
                    }
                    navHistory.put(fundCode, series);
                });

        return portfolioTrendCalculator.calculate(startDate, endDate, transactions, navHistory, latestPrices);
    }

    public List<DistributionItemVO> distribution(String username) {
        Map<String, BigDecimal> costMap = new LinkedHashMap<>();
        Map<String, BigDecimal> valueMap = new LinkedHashMap<>();
        for (PositionItemVO position : loadPositions(username)) {
            String category = position.fundType() == null ? "UNKNOWN" : position.fundType();
            costMap.merge(category, nvl(position.currentCost()), BigDecimal::add);
            valueMap.merge(category, nvl(position.marketValue()), BigDecimal::add);
        }
        return costMap.keySet().stream()
                .map(category -> new DistributionItemVO(
                        category,
                        costMap.getOrDefault(category, ZERO).setScale(2, RoundingMode.HALF_UP),
                        valueMap.getOrDefault(category, ZERO).setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(DistributionItemVO::marketValue).reversed())
                .toList();
    }

    public Map<String, List<RankingItemVO>> ranking(String username) {
        Comparator<PositionItemVO> comparator = Comparator.comparing(PositionItemVO::estimatedProfit);
        List<PositionItemVO> positions = loadPositions(username);

        List<RankingItemVO> profitTop = positions.stream()
                .sorted(comparator.reversed())
                .limit(5)
                .map(this::toRankingItem)
                .toList();

        List<RankingItemVO> lossTop = positions.stream()
                .sorted(comparator)
                .limit(5)
                .map(this::toRankingItem)
                .toList();

        return Map.of("profitTop", profitTop, "lossTop", lossTop);
    }

    private List<PositionItemVO> loadPositions(String username) {
        Long userId = currentUserService.getUserId(username);
        return positionService.listByUserId(userId);
    }

    private RankingItemVO toRankingItem(PositionItemVO position) {
        return new RankingItemVO(
                position.fundCode(),
                position.fundName(),
                position.estimatedProfit(),
                position.estimatedProfitRate()
        );
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(value -> value != null)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
