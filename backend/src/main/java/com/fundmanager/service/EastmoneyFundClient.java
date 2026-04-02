package com.fundmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class EastmoneyFundClient {

    private static final Duration SEARCH_TTL = Duration.ofMinutes(30);
    private static final Duration ESTIMATE_TTL = Duration.ofMinutes(10);
    private static final Duration DETAIL_TTL = Duration.ofHours(6);
    private static final Duration HOLDINGS_TTL = Duration.ofHours(12);

    private final RestTemplate restTemplate;
    private final RemoteValueCacheService cacheService;
    private final String searchUrlTemplate;
    private final String estimateUrlTemplate;
    private final String detailUrlTemplate;
    private final String holdingsUrlTemplate;

    public EastmoneyFundClient(RestTemplate restTemplate,
                               RemoteValueCacheService cacheService,
                               @Value("${app.quote.search-url}") String searchUrlTemplate,
                               @Value("${app.quote.estimate-url}") String estimateUrlTemplate,
                               @Value("${app.quote.detail-url}") String detailUrlTemplate,
                               @Value("${app.quote.holdings-url}") String holdingsUrlTemplate) {
        this.restTemplate = restTemplate;
        this.cacheService = cacheService;
        this.searchUrlTemplate = searchUrlTemplate;
        this.estimateUrlTemplate = estimateUrlTemplate;
        this.detailUrlTemplate = detailUrlTemplate;
        this.holdingsUrlTemplate = holdingsUrlTemplate;
    }

    public String search(String keyword) {
        String key = "eastmoney:search:" + md5(keyword.trim());
        return cacheService.getOrLoad(key, SEARCH_TTL, () ->
                request(String.format(searchUrlTemplate, encode(keyword.trim()))));
    }

    public String estimate(String fundCode) {
        String key = "eastmoney:estimate:" + fundCode;
        return cacheService.getOrLoad(key, ESTIMATE_TTL, () ->
                request(String.format(estimateUrlTemplate, fundCode)));
    }

    public String detailScript(String fundCode) {
        String key = "eastmoney:detail:" + fundCode;
        return cacheService.getOrLoad(key, DETAIL_TTL, () ->
                request(String.format(detailUrlTemplate, fundCode, Instant.now().toEpochMilli())));
    }

    public String holdings(String fundCode, int year, int quarter) {
        int month = switch (quarter) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            default -> throw new IllegalArgumentException("quarter must be 1-4");
        };
        String key = "eastmoney:holdings:%s:%d:%d".formatted(fundCode, year, quarter);
        return cacheService.getOrLoad(key, HOLDINGS_TTL, () ->
                request(String.format(holdingsUrlTemplate, fundCode, year, month)));
    }

    private String request(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 FundManager/1.0");
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        byte[] body = response.getBody();
        return body == null ? null : new String(body, StandardCharsets.UTF_8);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
