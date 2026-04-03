package com.fundmanager.service;

import com.fundmanager.domain.entity.FundHoldingSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class EquityHoldingEstimateStrategy implements FundEstimateStrategy {

    private static final BigDecimal MIN_HOLDING_COVERAGE = new BigDecimal("0.3000");
    private static final BigDecimal MAX_MISSING_QUOTE_RATIO = new BigDecimal("0.5000");
    private static final List<String> SUPPORTED_KEYWORDS = List.of("股票", "混合", "灵活配置", "指数增强");
    private static final List<String> FALLBACK_KEYWORDS = List.of("债券", "货币", "FOF", "QDII", "REIT", "联接");

    private final SecurityQuoteClient securityQuoteClient;

    public EquityHoldingEstimateStrategy(SecurityQuoteClient securityQuoteClient) {
        this.securityQuoteClient = securityQuoteClient;
    }

    @Override
    public boolean supports(FundEstimateContext context) {
        if (context.latestNav() == null || context.latestNav().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        String fundType = context.fundInfo() == null ? null : context.fundInfo().getFundType();
        if (!StringUtils.hasText(fundType)) {
            return false;
        }
        String normalized = fundType.toUpperCase();
        if (FALLBACK_KEYWORDS.stream().anyMatch(keyword -> normalized.contains(keyword.toUpperCase()))) {
            return false;
        }
        return SUPPORTED_KEYWORDS.stream().anyMatch(keyword -> normalized.contains(keyword.toUpperCase()));
    }

    @Override
    public Optional<FundEstimateComputation> estimate(FundEstimateContext context) {
        List<FundHoldingSnapshot> holdings = context.holdings();
        if (holdings == null || holdings.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal holdingCoverage = coverage(holdings);
        if (holdingCoverage.compareTo(MIN_HOLDING_COVERAGE) < 0) {
            return Optional.empty();
        }

        Map<String, SecurityQuoteSnapshot> quotes = securityQuoteClient.quotes(
                holdings.stream().map(FundHoldingSnapshot::getStockCode).toList()
        );

        BigDecimal quotedWeight = BigDecimal.ZERO;
        BigDecimal weightedReturn = BigDecimal.ZERO;

        for (FundHoldingSnapshot holding : holdings) {
            BigDecimal weight = toRatio(holding.getNavRatio());
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            SecurityQuoteSnapshot quote = quotes.get(holding.getStockCode());
            if (quote == null || quote.changeRate() == null) {
                continue;
            }
            quotedWeight = quotedWeight.add(weight);
            weightedReturn = weightedReturn.add(weight.multiply(quote.changeRate()));
        }

        if (quotedWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal missingQuoteRatio = holdingCoverage.subtract(quotedWeight).max(BigDecimal.ZERO);
        if (holdingCoverage.compareTo(BigDecimal.ZERO) > 0
                && missingQuoteRatio.divide(holdingCoverage, 4, RoundingMode.HALF_UP).compareTo(MAX_MISSING_QUOTE_RATIO) >= 0) {
            return Optional.empty();
        }

        BigDecimal estimateNav = context.latestNav()
                .multiply(BigDecimal.ONE.add(weightedReturn))
                .setScale(6, RoundingMode.HALF_UP);

        String confidence = holdingCoverage.compareTo(new BigDecimal("0.6000")) >= 0
                && quotedWeight.compareTo(new BigDecimal("0.5000")) >= 0
                ? "high"
                : "medium";

        return Optional.of(new FundEstimateComputation(
                context.fundCode(),
                estimateNav,
                weightedReturn.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP),
                LocalDateTime.now(),
                "self_holdings",
                confidence,
                holdingCoverage.setScale(4, RoundingMode.HALF_UP),
                quotedWeight.setScale(4, RoundingMode.HALF_UP),
                "eastmoney.push2"
        ));
    }

    private BigDecimal coverage(List<FundHoldingSnapshot> holdings) {
        BigDecimal total = BigDecimal.ZERO;
        for (FundHoldingSnapshot holding : holdings) {
            total = total.add(toRatio(holding.getNavRatio()));
        }
        return total.min(BigDecimal.ONE);
    }

    private BigDecimal toRatio(BigDecimal percentage) {
        if (percentage == null) {
            return BigDecimal.ZERO;
        }
        return percentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}
