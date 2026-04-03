package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.entity.FundEstimate;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.FundDetailVO;
import com.fundmanager.domain.vo.FundEstimateHistoryPointVO;
import com.fundmanager.domain.vo.FundEstimateVO;
import com.fundmanager.domain.vo.FundHoldingItemVO;
import com.fundmanager.domain.vo.FundNavVO;
import com.fundmanager.domain.vo.FundSearchItemVO;
import com.fundmanager.repository.FundEstimateRepository;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundNavRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FundService {

    private final FundInfoRepository fundInfoRepository;
    private final FundNavRepository navRepository;
    private final FundEstimateRepository estimateRepository;
    private final FundQuoteService quoteService;

    public FundService(FundInfoRepository fundInfoRepository,
                       FundNavRepository navRepository,
                       FundEstimateRepository estimateRepository,
                       FundQuoteService quoteService) {
        this.fundInfoRepository = fundInfoRepository;
        this.navRepository = navRepository;
        this.estimateRepository = estimateRepository;
        this.quoteService = quoteService;
    }

    public List<FundSearchItemVO> search(String keyword) {
        return quoteService.search(keyword);
    }

    public FundEstimateVO getEstimate(String fundCode) {
        FundEstimate estimate = quoteService.refreshEstimate(fundCode)
                .orElseGet(() -> estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode)
                        .orElseThrow(() -> new BusinessException("未找到基金估值数据")));

        String fundName = fundInfoRepository.findByFundCode(fundCode)
                .map(FundInfo::getFundName)
                .orElse(fundCode);

        return new FundEstimateVO(
                estimate.getFundCode(),
                fundName,
                estimate.getEstimateNav(),
                estimate.getEstimateGrowthRate(),
                estimate.getEstimateTime(),
                estimate.getEstimateSource(),
                estimate.getEstimateConfidence(),
                estimate.getHoldingCoverageRate(),
                estimate.getQuotedCoverageRate(),
                estimate.getEstimateTime()
        );
    }

    public FundDetailVO getDetail(String fundCode) {
        Optional<EastmoneyFundDetailPayload> remoteDetail = quoteService.refreshDetail(fundCode);
        FundQuoteSnapshot snapshot = quoteService.loadSnapshot(fundCode, false);
        FundInfo fundInfo = fundInfoRepository.findByFundCode(fundCode).orElse(null);

        if (fundInfo == null && remoteDetail.isEmpty() && snapshot.currentNav().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("基金不存在");
        }

        return new FundDetailVO(
                fundCode,
                fundInfo == null ? snapshot.fundName() : fundInfo.getFundName(),
                fundInfo == null ? "UNKNOWN" : fundInfo.getFundType(),
                fundInfo == null ? null : fundInfo.getRiskLevel(),
                fundInfo == null ? null : fundInfo.getManagementCompany(),
                snapshot.latestNav(),
                snapshot.latestNavDate(),
                snapshot.currentNav(),
                snapshot.estimateGrowthRate(),
                snapshot.estimateTime(),
                snapshot.estimateSource(),
                snapshot.estimateConfidence(),
                snapshot.holdingCoverageRate(),
                snapshot.quotedCoverageRate(),
                snapshot.estimateTime(),
                remoteDetail.map(EastmoneyFundDetailPayload::sourceRate).orElse(null),
                remoteDetail.map(EastmoneyFundDetailPayload::currentRate).orElse(null),
                remoteDetail.map(EastmoneyFundDetailPayload::minPurchaseAmount).orElse(null),
                remoteDetail.map(EastmoneyFundDetailPayload::returnStats).orElse(List.of()),
                remoteDetail.map(EastmoneyFundDetailPayload::performanceRadar)
                        .orElseGet(() -> new com.fundmanager.domain.vo.FundPerformanceRadarVO(null, List.of(), List.of())),
                remoteDetail.map(EastmoneyFundDetailPayload::managers).orElse(List.of()),
                remoteDetail.map(EastmoneyFundDetailPayload::assetAllocation)
                        .orElseGet(() -> new com.fundmanager.domain.vo.FundChartBlockVO(List.of(), List.of())),
                remoteDetail.map(EastmoneyFundDetailPayload::holderStructure)
                        .orElseGet(() -> new com.fundmanager.domain.vo.FundChartBlockVO(List.of(), List.of())),
                remoteDetail.map(EastmoneyFundDetailPayload::scaleTrend).orElse(List.of()),
                remoteDetail.map(EastmoneyFundDetailPayload::sameTypeReferences).orElse(List.of())
        );
    }

    public List<FundNavVO> navHistory(String fundCode, String range) {
        quoteService.refreshDetail(fundCode);
        List<FundNav> navList = switch (normalizeRange(range)) {
            case "1m" -> navRepository.findByFundCodeAndNavDateGreaterThanEqualOrderByNavDateAsc(
                    fundCode,
                    LocalDate.now().minusMonths(1)
            );
            case "3m" -> navRepository.findByFundCodeAndNavDateGreaterThanEqualOrderByNavDateAsc(
                    fundCode,
                    LocalDate.now().minusMonths(3)
            );
            case "6m" -> navRepository.findByFundCodeAndNavDateGreaterThanEqualOrderByNavDateAsc(
                    fundCode,
                    LocalDate.now().minusMonths(6)
            );
            case "1y" -> navRepository.findByFundCodeAndNavDateGreaterThanEqualOrderByNavDateAsc(
                    fundCode,
                    LocalDate.now().minusYears(1)
            );
            default -> navRepository.findByFundCodeOrderByNavDateAsc(fundCode);
        };

        return navList.stream()
                .map(item -> new FundNavVO(
                        item.getNavDate(),
                        item.getUnitNav(),
                        item.getAccumulatedNav(),
                        item.getDailyGrowthRate()
                ))
                .toList();
    }

    public List<FundEstimateHistoryPointVO> estimateHistory(String fundCode, LocalDate date) {
        return quoteService.loadEstimateHistory(fundCode, date);
    }

    public List<FundHoldingItemVO> holdings(String fundCode, Integer year, Integer quarter) {
        int resolvedQuarter = quarter == null ? currentQuarter(LocalDate.now()) : quarter;
        if (resolvedQuarter < 1 || resolvedQuarter > 4) {
            throw new BusinessException("季度参数必须在 1 到 4 之间");
        }

        int resolvedYear = year == null ? LocalDate.now().getYear() : year;
        return quoteService.loadHoldings(fundCode, resolvedYear, resolvedQuarter);
    }

    private String normalizeRange(String range) {
        if (!StringUtils.hasText(range)) {
            return "6m";
        }
        return range.trim().toLowerCase();
    }

    private int currentQuarter(LocalDate date) {
        return ((date.getMonthValue() - 1) / 3) + 1;
    }
}
