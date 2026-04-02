package com.fundmanager.service;

import com.fundmanager.common.BusinessException;
import com.fundmanager.domain.entity.FundEstimate;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.FundDetailVO;
import com.fundmanager.domain.vo.FundEstimateVO;
import com.fundmanager.domain.vo.FundNavVO;
import com.fundmanager.domain.vo.FundSearchItemVO;
import com.fundmanager.repository.FundEstimateRepository;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundNavRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
        String key = keyword == null ? "" : keyword.trim();
        List<FundInfo> list = fundInfoRepository.findTop20ByFundCodeContainingOrFundNameContainingOrderByFundCodeAsc(key, key);
        if (list.isEmpty() && key.matches("\\d{6}")) {
            quoteService.refreshEstimate(key);
            list = fundInfoRepository.findTop20ByFundCodeContainingOrFundNameContainingOrderByFundCodeAsc(key, key);
        }
        return list.stream()
                .map(it -> new FundSearchItemVO(it.getFundCode(), it.getFundName(), it.getFundType()))
                .toList();
    }

    public FundEstimateVO getEstimate(String fundCode) {
        quoteService.refreshEstimate(fundCode);
        FundEstimate estimate = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode)
                .orElseThrow(() -> new BusinessException("Estimate data not found"));
        String fundName = fundInfoRepository.findByFundCode(fundCode).map(FundInfo::getFundName).orElse(fundCode);
        return new FundEstimateVO(estimate.getFundCode(), fundName, estimate.getEstimateNav(),
                estimate.getEstimateGrowthRate(), estimate.getEstimateTime());
    }

    public FundDetailVO getDetail(String fundCode) {
        FundInfo fundInfo = fundInfoRepository.findByFundCode(fundCode)
                .orElseThrow(() -> new BusinessException("Fund not found"));
        Optional<FundNav> latestNav = navRepository.findByFundCodeOrderByNavDateDesc(fundCode, PageRequest.of(0, 1))
                .stream().findFirst();
        Optional<FundEstimate> latestEstimate = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode);

        return new FundDetailVO(
                fundInfo.getFundCode(),
                fundInfo.getFundName(),
                fundInfo.getFundType(),
                fundInfo.getRiskLevel(),
                fundInfo.getManagementCompany(),
                latestNav.map(FundNav::getUnitNav).orElse(null),
                latestNav.map(FundNav::getNavDate).orElse(null),
                latestEstimate.map(FundEstimate::getEstimateNav).orElse(null),
                latestEstimate.map(FundEstimate::getEstimateGrowthRate).orElse(null),
                latestEstimate.map(FundEstimate::getEstimateTime).orElse(null)
        );
    }

    public List<FundNavVO> navHistory(String fundCode, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 60));
        List<FundNav> navList = navRepository.findByFundCodeOrderByNavDateDesc(fundCode, PageRequest.of(0, pageSize));
        return navList.stream()
                .map(it -> new FundNavVO(it.getNavDate(), it.getUnitNav(), it.getAccumulatedNav(), it.getDailyGrowthRate()))
                .toList();
    }
}
