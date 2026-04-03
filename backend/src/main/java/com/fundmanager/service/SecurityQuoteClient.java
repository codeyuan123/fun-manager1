package com.fundmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecurityQuoteClient {

    private static final Duration QUOTE_TTL = Duration.ofMinutes(1);
    private static final String QUOTE_URL = "https://push2.eastmoney.com/api/qt/ulist.np/get";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RemoteValueCacheService cacheService;

    public SecurityQuoteClient(RestTemplate restTemplate,
                               ObjectMapper objectMapper,
                               RemoteValueCacheService cacheService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }

    public Map<String, SecurityQuoteSnapshot> quotes(Collection<String> securityCodes) {
        List<String> normalizedCodes = securityCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, String> secidMap = new LinkedHashMap<>();
        for (String code : normalizedCodes) {
            resolveSecid(code).ifPresent(secid -> secidMap.put(code, secid));
        }
        if (secidMap.isEmpty()) {
            return Map.of();
        }

        String key = "eastmoney:security-quotes:" + String.join(",", secidMap.keySet());
        String body = cacheService.getOrLoad(key, QUOTE_TTL, () -> request(secidMap.values()));
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            Map<String, SecurityQuoteSnapshot> result = new HashMap<>();
            for (JsonNode item : root.path("data").path("diff")) {
                String code = item.path("f12").asText();
                if (code == null || code.isBlank()) {
                    continue;
                }
                BigDecimal price = parseScaledDecimal(item.path("f2").asText(), 100);
                // Eastmoney f3 is percentage * 100, convert it to a decimal ratio.
                BigDecimal changeRate = parseScaledDecimal(item.path("f3").asText(), 10000);
                result.put(code, new SecurityQuoteSnapshot(
                        code,
                        item.path("f14").asText(code),
                        price,
                        changeRate
                ));
            }
            return result;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String request(Collection<String> secids) {
        String url = UriComponentsBuilder.fromHttpUrl(QUOTE_URL)
                .queryParam("secids", secids.stream().collect(Collectors.joining(",")))
                .queryParam("fields", "f2,f3,f12,f14")
                .build(false)
                .toUriString();
        return restTemplate.getForObject(url, String.class);
    }

    private Optional<String> resolveSecid(String code) {
        if (code.matches("^(60|68|90|50|51|52|56|58)\\d+$")) {
            return Optional.of("1." + code);
        }
        if (code.matches("^(00|30|15|16|18|20|43|83|87|88|92)\\d+$")) {
            return Optional.of("0." + code);
        }
        if (code.matches("^\\d{5}$")) {
            return Optional.of("116." + code);
        }
        return Optional.empty();
    }

    private BigDecimal parseScaledDecimal(String raw, int scaleFactor) {
        if (raw == null || raw.isBlank() || "-".equals(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw).divide(BigDecimal.valueOf(scaleFactor), 4, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
