package com.fundmanager.service;

import com.fundmanager.domain.entity.FundEstimate;
import com.fundmanager.domain.entity.FundHoldingSnapshot;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.domain.vo.FundHoldingItemVO;
import com.fundmanager.domain.vo.FundSearchItemVO;
import com.fundmanager.repository.FundEstimateRepository;
import com.fundmanager.repository.FundHoldingSnapshotRepository;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundNavRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FundQuoteService {

    private final EastmoneyFundClient eastmoneyFundClient;
    private final EastmoneyFundParser eastmoneyFundParser;
    private final FundEstimateRepository estimateRepository;
    private final FundNavRepository navRepository;
    private final FundInfoRepository fundInfoRepository;
    private final FundHoldingSnapshotRepository holdingSnapshotRepository;

    public FundQuoteService(EastmoneyFundClient eastmoneyFundClient,
                            EastmoneyFundParser eastmoneyFundParser,
                            FundEstimateRepository estimateRepository,
                            FundNavRepository navRepository,
                            FundInfoRepository fundInfoRepository,
                            FundHoldingSnapshotRepository holdingSnapshotRepository) {
        this.eastmoneyFundClient = eastmoneyFundClient;
        this.eastmoneyFundParser = eastmoneyFundParser;
        this.estimateRepository = estimateRepository;
        this.navRepository = navRepository;
        this.fundInfoRepository = fundInfoRepository;
        this.holdingSnapshotRepository = holdingSnapshotRepository;
    }

    public List<FundSearchItemVO> search(String keyword) {
        String key = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(key)) {
            return searchLocal(key);
        }

        try {
            List<EastmoneySearchItem> remoteItems =
                    eastmoneyFundParser.parseSearchResponse(eastmoneyFundClient.search(key));
            remoteItems.forEach(this::upsertFundInfo);
            return remoteItems.stream()
                    .map(item -> new FundSearchItemVO(item.fundCode(), item.fundName(), item.fundType()))
                    .toList();
        } catch (Exception ignored) {
            return searchLocal(key);
        }
    }

    public Optional<FundEstimate> refreshEstimate(String fundCode) {
        Optional<FundEstimate> latest = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode);
        try {
            EastmoneyEstimatePayload payload = eastmoneyFundParser.parseEstimateResponse(
                    fundCode,
                    eastmoneyFundClient.estimate(fundCode)
            );
            if (payload.estimateNav() == null) {
                return latest;
            }

            upsertFundInfo(payload.fundCode(), payload.fundName(), null, null);
            FundEstimate saved = saveEstimate(payload);
            return Optional.of(saved);
        } catch (Exception ignored) {
            return latest;
        }
    }

    public Optional<EastmoneyFundDetailPayload> refreshDetail(String fundCode) {
        try {
            EastmoneyFundDetailPayload payload = eastmoneyFundParser.parseDetailScript(
                    eastmoneyFundClient.detailScript(fundCode)
            );
            String resolvedCode = StringUtils.hasText(payload.fundCode()) ? payload.fundCode() : fundCode;
            upsertSearchInfoByCode(resolvedCode);
            upsertFundInfo(resolvedCode, payload.fundName(), null, null);
            saveNavHistory(resolvedCode, payload.navPoints());
            return Optional.of(payload);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public List<FundHoldingItemVO> loadHoldings(String fundCode, int year, int quarter) {
        byte quarterNum = (byte) quarter;
        try {
            EastmoneyHoldingPayload payload = eastmoneyFundParser.parseHoldingsResponse(
                    fundCode,
                    year,
                    quarter,
                    eastmoneyFundClient.holdings(fundCode, year, quarter)
            );
            saveHoldings(payload);
            return payload.items();
        } catch (Exception ignored) {
            return holdingSnapshotRepository.findByFundCodeAndYearNumAndQuarterNumOrderByNavRatioDesc(
                            fundCode,
                            year,
                            quarterNum
                    ).stream()
                    .map(item -> new FundHoldingItemVO(
                            item.getStockCode(),
                            item.getStockName(),
                            item.getNavRatio(),
                            item.getHoldingShares(),
                            item.getHoldingMarketValue(),
                            item.getReportDate() == null ? null : item.getReportDate().toString()
                    ))
                    .toList();
        }
    }

    public FundQuoteSnapshot loadSnapshot(String fundCode, boolean forceRefresh) {
        FundInfo infoBefore = fundInfoRepository.findByFundCode(fundCode).orElse(null);
        if (forceRefresh || needsMetadata(infoBefore) || navMissing(fundCode)) {
            refreshDetail(fundCode);
            upsertSearchInfoByCode(fundCode);
        }

        Optional<FundEstimate> latestEstimate = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode);
        if (forceRefresh || latestEstimate.isEmpty() || isEstimateStale(latestEstimate.get())) {
            Optional<FundEstimate> refreshed = refreshEstimate(fundCode);
            if (refreshed.isPresent()) {
                latestEstimate = refreshed;
            }
        }

        List<FundNav> navList = navRepository.findByFundCodeOrderByNavDateDesc(fundCode, PageRequest.of(0, 2));
        FundNav latestNav = navList.isEmpty() ? null : navList.get(0);
        FundNav previousNav = navList.size() > 1 ? navList.get(1) : null;
        FundInfo info = fundInfoRepository.findByFundCode(fundCode).orElse(null);

        BigDecimal currentNav = latestEstimate.map(FundEstimate::getEstimateNav)
                .orElse(latestNav == null ? BigDecimal.ZERO : latestNav.getUnitNav());

        return new FundQuoteSnapshot(
                fundCode,
                resolveFundName(info, fundCode),
                currentNav,
                latestNav == null ? BigDecimal.ZERO : latestNav.getUnitNav(),
                latestNav == null ? null : latestNav.getNavDate(),
                previousNav == null ? null : previousNav.getUnitNav(),
                latestEstimate.map(FundEstimate::getEstimateGrowthRate).orElse(BigDecimal.ZERO),
                latestEstimate.map(FundEstimate::getEstimateTime).orElse(null)
        );
    }

    private List<FundSearchItemVO> searchLocal(String keyword) {
        String key = keyword == null ? "" : keyword;
        return fundInfoRepository.findTop20ByFundCodeContainingOrFundNameContainingOrderByFundCodeAsc(key, key).stream()
                .map(item -> new FundSearchItemVO(item.getFundCode(), item.getFundName(), item.getFundType()))
                .toList();
    }

    private FundEstimate saveEstimate(EastmoneyEstimatePayload payload) {
        LocalDateTime estimateTime = payload.estimateTime() == null ? LocalDateTime.now() : payload.estimateTime();
        Optional<FundEstimate> latest = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(payload.fundCode());
        if (latest.isPresent()
                && estimateTime.equals(latest.get().getEstimateTime())
                && decimalEquals(payload.estimateNav(), latest.get().getEstimateNav())
                && decimalEquals(payload.estimateGrowthRate(), latest.get().getEstimateGrowthRate())) {
            return latest.get();
        }

        FundEstimate estimate = new FundEstimate();
        estimate.setFundCode(payload.fundCode());
        estimate.setEstimateTime(estimateTime);
        estimate.setEstimateNav(payload.estimateNav());
        estimate.setEstimateGrowthRate(payload.estimateGrowthRate());
        estimate.setSource("fundgz.1234567.com.cn");
        estimate.setCreatedAt(LocalDateTime.now());
        return estimateRepository.save(estimate);
    }

    private void saveNavHistory(String fundCode, List<EastmoneyNavPoint> navPoints) {
        if (navPoints == null || navPoints.isEmpty()) {
            return;
        }

        Map<java.time.LocalDate, FundNav> existingMap = new LinkedHashMap<>();
        for (FundNav existing : navRepository.findByFundCodeOrderByNavDateAsc(fundCode)) {
            existingMap.put(existing.getNavDate(), existing);
        }

        List<FundNav> toSave = new ArrayList<>();
        for (EastmoneyNavPoint point : navPoints) {
            FundNav nav = existingMap.get(point.date());
            if (nav == null) {
                nav = new FundNav();
                nav.setFundCode(fundCode);
                nav.setNavDate(point.date());
                nav.setCreatedAt(LocalDateTime.now());
            }

            if (!decimalEquals(nav.getUnitNav(), point.unitNav())
                    || !decimalEquals(nav.getAccumulatedNav(), point.accumulatedNav())
                    || !decimalEquals(nav.getDailyGrowthRate(), point.dailyGrowthRate())
                    || !StringUtils.hasText(nav.getSource())) {
                nav.setUnitNav(point.unitNav());
                nav.setAccumulatedNav(point.accumulatedNav());
                nav.setDailyGrowthRate(point.dailyGrowthRate());
                nav.setSource("fund.eastmoney.com");
                toSave.add(nav);
            }
        }

        if (!toSave.isEmpty()) {
            navRepository.saveAll(toSave);
        }
    }

    private void saveHoldings(EastmoneyHoldingPayload payload) {
        holdingSnapshotRepository.deleteByFundCodeAndYearNumAndQuarterNum(
                payload.fundCode(),
                payload.year(),
                payload.quarter().byteValue()
        );

        if (payload.items() == null || payload.items().isEmpty()) {
            return;
        }

        List<FundHoldingSnapshot> snapshots = new ArrayList<>();
        for (FundHoldingItemVO item : payload.items()) {
            FundHoldingSnapshot snapshot = new FundHoldingSnapshot();
            snapshot.setFundCode(payload.fundCode());
            snapshot.setYearNum(payload.year());
            snapshot.setQuarterNum(payload.quarter().byteValue());
            snapshot.setReportDate(payload.reportDate());
            snapshot.setStockCode(item.stockCode());
            snapshot.setStockName(item.stockName());
            snapshot.setNavRatio(item.navRatio());
            snapshot.setHoldingShares(item.holdingShares());
            snapshot.setHoldingMarketValue(item.holdingMarketValue());
            snapshot.setCreatedAt(LocalDateTime.now());
            snapshots.add(snapshot);
        }
        holdingSnapshotRepository.saveAll(snapshots);
    }

    private void upsertSearchInfoByCode(String fundCode) {
        try {
            List<EastmoneySearchItem> items = eastmoneyFundParser.parseSearchResponse(eastmoneyFundClient.search(fundCode));
            items.stream()
                    .filter(item -> fundCode.equals(item.fundCode()))
                    .findFirst()
                    .ifPresent(this::upsertFundInfo);
        } catch (Exception ignored) {
        }
    }

    private void upsertFundInfo(EastmoneySearchItem item) {
        upsertFundInfo(item.fundCode(), item.fundName(), item.fundType(), item.managementCompany());
    }

    private void upsertFundInfo(String fundCode, String fundName, String fundType, String managementCompany) {
        if (!StringUtils.hasText(fundCode)) {
            return;
        }

        FundInfo info = fundInfoRepository.findByFundCode(fundCode).orElseGet(FundInfo::new);
        boolean isNew = info.getId() == null;
        if (isNew) {
            info.setFundCode(fundCode);
            info.setCreatedAt(LocalDateTime.now());
            info.setStatus((byte) 1);
        }

        if (StringUtils.hasText(fundName)) {
            info.setFundName(fundName.trim());
        } else if (!StringUtils.hasText(info.getFundName())) {
            info.setFundName(fundCode);
        }

        if (StringUtils.hasText(fundType)) {
            info.setFundType(fundType.trim());
        } else if (!StringUtils.hasText(info.getFundType())) {
            info.setFundType("UNKNOWN");
        }

        if (StringUtils.hasText(managementCompany)) {
            info.setManagementCompany(managementCompany.trim());
        }

        info.setUpdatedAt(LocalDateTime.now());
        fundInfoRepository.save(info);
    }

    private boolean navMissing(String fundCode) {
        return navRepository.findByFundCodeOrderByNavDateDesc(fundCode, PageRequest.of(0, 1)).isEmpty();
    }

    private boolean needsMetadata(FundInfo info) {
        return info == null
                || !StringUtils.hasText(info.getFundName())
                || !StringUtils.hasText(info.getFundType())
                || "UNKNOWN".equalsIgnoreCase(info.getFundType());
    }

    private boolean isEstimateStale(FundEstimate estimate) {
        return estimate.getEstimateTime() == null
                || estimate.getEstimateTime().isBefore(LocalDateTime.now().minusMinutes(10));
    }

    private String resolveFundName(FundInfo info, String fundCode) {
        if (info != null && StringUtils.hasText(info.getFundName())) {
            return info.getFundName();
        }
        return fundCode;
    }

    private boolean decimalEquals(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }
}
