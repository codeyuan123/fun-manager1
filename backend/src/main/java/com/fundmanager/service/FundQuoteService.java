package com.fundmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundmanager.domain.entity.FundEstimate;
import com.fundmanager.domain.entity.FundInfo;
import com.fundmanager.domain.entity.FundNav;
import com.fundmanager.repository.FundEstimateRepository;
import com.fundmanager.repository.FundInfoRepository;
import com.fundmanager.repository.FundNavRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FundQuoteService {

    private static final Pattern JSONP_PATTERN = Pattern.compile("^\\w+\\((.*)\\);?$");
    private static final DateTimeFormatter GZ_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FundEstimateRepository estimateRepository;
    private final FundNavRepository navRepository;
    private final FundInfoRepository fundInfoRepository;
    private final String estimateUrlTemplate;

    public FundQuoteService(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            FundEstimateRepository estimateRepository,
                            FundNavRepository navRepository,
                            FundInfoRepository fundInfoRepository,
                            @Value("${app.quote.estimate-url}") String estimateUrlTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.estimateRepository = estimateRepository;
        this.navRepository = navRepository;
        this.fundInfoRepository = fundInfoRepository;
        this.estimateUrlTemplate = estimateUrlTemplate;
    }

    public Optional<FundEstimate> refreshEstimate(String fundCode) {
        try {
            String body = restTemplate.getForObject(String.format(estimateUrlTemplate, fundCode), String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            Matcher matcher = JSONP_PATTERN.matcher(body.trim());
            if (!matcher.matches()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(matcher.group(1));
            String code = root.path("fundcode").asText(fundCode);
            String name = root.path("name").asText(code);
            BigDecimal estimateNav = parseBigDecimal(root.path("gsz").asText());
            BigDecimal growthRate = parseBigDecimal(root.path("gszzl").asText());
            String estimateTimeText = root.path("gztime").asText();
            LocalDateTime estimateTime = estimateTimeText == null || estimateTimeText.isBlank()
                    ? LocalDateTime.now()
                    : LocalDateTime.parse(estimateTimeText, GZ_TIME_FORMATTER);

            if (estimateNav == null) {
                return Optional.empty();
            }

            FundEstimate estimate = new FundEstimate();
            estimate.setFundCode(code);
            estimate.setEstimateNav(estimateNav);
            estimate.setEstimateGrowthRate(growthRate);
            estimate.setEstimateTime(estimateTime);
            estimate.setSource("fundgz.1234567.com.cn");
            estimate.setCreatedAt(LocalDateTime.now());
            estimate = estimateRepository.save(estimate);

            FundInfo info = fundInfoRepository.findByFundCode(code).orElseGet(FundInfo::new);
            if (info.getId() == null) {
                info.setFundCode(code);
                info.setCreatedAt(LocalDateTime.now());
                info.setStatus((byte) 1);
            }
            info.setFundName(name);
            if (info.getFundType() == null) {
                info.setFundType("UNKNOWN");
            }
            info.setUpdatedAt(LocalDateTime.now());
            fundInfoRepository.save(info);

            return Optional.of(estimate);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public FundQuoteSnapshot loadSnapshot(String fundCode, boolean forceRefresh) {
        Optional<FundEstimate> latestEstimate = estimateRepository.findTopByFundCodeOrderByEstimateTimeDesc(fundCode);
        if (forceRefresh || latestEstimate.isEmpty()
                || latestEstimate.get().getEstimateTime().isBefore(LocalDateTime.now().minusMinutes(10))) {
            Optional<FundEstimate> refreshed = refreshEstimate(fundCode);
            if (refreshed.isPresent()) {
                latestEstimate = refreshed;
            }
        }

        List<FundNav> navList = navRepository.findByFundCodeOrderByNavDateDesc(fundCode, PageRequest.of(0, 2));
        FundNav latestNav = navList.isEmpty() ? null : navList.get(0);
        FundNav previousNav = navList.size() > 1 ? navList.get(1) : null;

        String fundName = fundInfoRepository.findByFundCode(fundCode)
                .map(FundInfo::getFundName)
                .orElse(fundCode);
        BigDecimal estimateNav = latestEstimate.map(FundEstimate::getEstimateNav).orElse(null);
        BigDecimal currentNav = estimateNav != null
                ? estimateNav
                : latestNav != null ? latestNav.getUnitNav() : BigDecimal.ZERO;

        return new FundQuoteSnapshot(
                fundCode,
                fundName,
                currentNav,
                latestNav == null ? BigDecimal.ZERO : latestNav.getUnitNav(),
                latestNav == null ? null : latestNav.getNavDate(),
                previousNav == null ? null : previousNav.getUnitNav(),
                latestEstimate.map(FundEstimate::getEstimateGrowthRate).orElse(BigDecimal.ZERO),
                latestEstimate.map(FundEstimate::getEstimateTime).orElse(null)
        );
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return new BigDecimal(value);
    }
}

