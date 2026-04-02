package com.fundmanager.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fundmanager.domain.vo.FundChartBlockVO;
import com.fundmanager.domain.vo.FundChartSeriesVO;
import com.fundmanager.domain.vo.FundHoldingItemVO;
import com.fundmanager.domain.vo.FundManagerCardVO;
import com.fundmanager.domain.vo.FundPeerReferenceVO;
import com.fundmanager.domain.vo.FundPerformanceRadarVO;
import com.fundmanager.domain.vo.FundReturnStatVO;
import com.fundmanager.domain.vo.FundScalePointVO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EastmoneyFundParser {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ESTIMATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern JSONP_PATTERN = Pattern.compile("^\\w+\\((.*)\\);?$", Pattern.DOTALL);

    private final ObjectMapper jsonMapper;

    public EastmoneyFundParser(ObjectMapper objectMapper) {
        this.jsonMapper = objectMapper.copy()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
    }

    public List<EastmoneySearchItem> parseSearchResponse(String body) {
        try {
            JsonNode root = jsonMapper.readTree(body);
            List<EastmoneySearchItem> result = new ArrayList<>();
            for (JsonNode item : root.path("Datas")) {
                JsonNode baseInfo = item.path("FundBaseInfo");
                String code = text(item, "CODE", text(baseInfo, "FCODE", ""));
                if (!StringUtils.hasText(code)) {
                    continue;
                }
                result.add(new EastmoneySearchItem(
                        code,
                        text(item, "NAME", text(baseInfo, "SHORTNAME", code)),
                        text(baseInfo, "FTYPE", text(item, "CATEGORYDESC", "UNKNOWN")),
                        text(baseInfo, "JJGS", null)
                ));
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse Eastmoney search response", ex);
        }
    }

    public EastmoneyEstimatePayload parseEstimateResponse(String fundCode, String body) {
        Matcher matcher = JSONP_PATTERN.matcher(body == null ? "" : body.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException("Estimate payload is not valid JSONP");
        }
        try {
            JsonNode root = jsonMapper.readTree(matcher.group(1));
            return new EastmoneyEstimatePayload(
                    text(root, "fundcode", fundCode),
                    text(root, "name", fundCode),
                    parseDecimal(root.path("gsz").asText()),
                    parseDecimal(root.path("gszzl").asText()),
                    parseDateTime(root.path("gztime").asText())
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse Eastmoney estimate response", ex);
        }
    }

    public EastmoneyFundDetailPayload parseDetailScript(String script) {
        return new EastmoneyFundDetailPayload(
                extractQuotedValue(script, "fS_code"),
                extractQuotedValue(script, "fS_name"),
                parseDecimal(extractQuotedValue(script, "fund_sourceRate")),
                parseDecimal(extractQuotedValue(script, "fund_Rate")),
                parseDecimal(extractQuotedValue(script, "fund_minsg")),
                parseReturnStats(script),
                parseRadarBlock(script, "Data_performanceEvaluation"),
                parseManagerCards(script),
                parseChartBlock(script, "Data_assetAllocation"),
                parseChartBlock(script, "Data_holderStructure"),
                parseScaleTrend(script),
                parsePeerReferences(script),
                parseNavPoints(script)
        );
    }

    public EastmoneyHoldingPayload parseHoldingsResponse(String fundCode, int year, int quarter, String body) {
        String content = extractContent(body);
        Document document = Jsoup.parse(unescapeContent(content));
        Elements boxes = document.select("div.boxitem");

        Element target = null;
        for (Element box : boxes) {
            if (matchesQuarter(box, year, quarter)) {
                target = box;
                break;
            }
        }
        if (target == null) {
            target = boxes.first();
        }
        if (target == null) {
            return new EastmoneyHoldingPayload(fundCode, year, quarter, null, List.of());
        }

        LocalDate reportDate = null;
        String reportDateText = target.select("h4 .right font").text();
        if (StringUtils.hasText(reportDateText)) {
            reportDate = LocalDate.parse(reportDateText.trim(), REPORT_DATE_FORMATTER);
        }

        List<FundHoldingItemVO> items = new ArrayList<>();
        for (Element row : target.select("tbody tr")) {
            Elements cols = row.select("td");
            if (cols.size() < 5) {
                continue;
            }
            String stockCode = cols.get(1).text().trim();
            String stockName = cols.get(2).text().trim();
            BigDecimal navRatio = parseNumberText(cols.get(cols.size() - 3).text());
            BigDecimal holdingShares = parseNumberText(cols.get(cols.size() - 2).text());
            BigDecimal holdingMarketValue = parseNumberText(cols.get(cols.size() - 1).text());
            items.add(new FundHoldingItemVO(
                    stockCode,
                    stockName,
                    navRatio,
                    holdingShares,
                    holdingMarketValue,
                    reportDate == null ? null : reportDate.toString()
            ));
        }

        return new EastmoneyHoldingPayload(fundCode, year, quarter, reportDate, items);
    }

    private List<EastmoneyNavPoint> parseNavPoints(String script) {
        JsonNode unitTrend = parseJsonAssignment(script, "Data_netWorthTrend");
        JsonNode accTrend = parseOptionalJsonAssignment(script, "Data_ACWorthTrend");
        Map<LocalDate, BigDecimal> accumulatedMap = new HashMap<>();
        if (accTrend != null && accTrend.isArray()) {
            for (JsonNode item : accTrend) {
                if (item.isArray() && item.size() >= 2) {
                    accumulatedMap.put(toDate(item.get(0).asLong()), parseDecimalNode(item.get(1)));
                }
            }
        }

        List<EastmoneyNavPoint> result = new ArrayList<>();
        for (JsonNode item : unitTrend) {
            LocalDate date = toDate(item.path("x").asLong());
            BigDecimal unitNav = parseDecimalNode(item.path("y"));
            BigDecimal accumulatedNav = accumulatedMap.getOrDefault(date, unitNav);
            BigDecimal growthRate = parseDecimalNode(item.path("equityReturn"));
            result.add(new EastmoneyNavPoint(date, unitNav, accumulatedNav, growthRate));
        }
        return result;
    }

    private List<FundReturnStatVO> parseReturnStats(String script) {
        return List.of(
                new FundReturnStatVO("1M", parseDecimal(extractQuotedValue(script, "syl_1y"))),
                new FundReturnStatVO("3M", parseDecimal(extractQuotedValue(script, "syl_3y"))),
                new FundReturnStatVO("6M", parseDecimal(extractQuotedValue(script, "syl_6y"))),
                new FundReturnStatVO("1Y", parseDecimal(extractQuotedValue(script, "syl_1n")))
        );
    }

    private FundPerformanceRadarVO parseRadarBlock(String script, String variableName) {
        JsonNode node = parseOptionalJsonAssignment(script, variableName);
        if (node == null || node.isMissingNode()) {
            return new FundPerformanceRadarVO(null, List.of(), List.of());
        }
        return new FundPerformanceRadarVO(
                text(node, "avr", null),
                toStringList(node.path("categories")),
                toNumberList(node.path("data"))
        );
    }

    private List<FundManagerCardVO> parseManagerCards(String script) {
        JsonNode node = parseOptionalJsonAssignment(script, "Data_currentFundManager");
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<FundManagerCardVO> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(new FundManagerCardVO(
                    text(item, "id", null),
                    text(item, "name", null),
                    text(item, "pic", null),
                    item.path("star").isNumber() ? item.path("star").asInt() : null,
                    text(item, "workTime", null),
                    text(item, "fundSize", null),
                    parseRadarNode(item.path("power")),
                    parseProfitComparison(item.path("profit"))
            ));
        }
        return result;
    }

    private FundChartBlockVO parseProfitComparison(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return new FundChartBlockVO(List.of(), List.of());
        }

        List<String> categories = toStringList(node.path("categories"));
        List<BigDecimal> values = new ArrayList<>();
        JsonNode series = node.path("series");
        if (series.isArray() && !series.isEmpty()) {
            for (JsonNode item : series.get(0).path("data")) {
                values.add(parseDecimalNode(item.path("y")));
            }
        }
        return new FundChartBlockVO(categories, List.of(new FundChartSeriesVO("Relative", values)));
    }

    private FundPerformanceRadarVO parseRadarNode(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return new FundPerformanceRadarVO(null, List.of(), List.of());
        }
        return new FundPerformanceRadarVO(
                text(node, "avr", null),
                toStringList(node.path("categories")),
                toNumberList(node.path("data"))
        );
    }

    private FundChartBlockVO parseChartBlock(String script, String variableName) {
        JsonNode node = parseOptionalJsonAssignment(script, variableName);
        if (node == null || node.isMissingNode()) {
            return new FundChartBlockVO(List.of(), List.of());
        }

        List<FundChartSeriesVO> series = new ArrayList<>();
        for (JsonNode seriesNode : node.path("series")) {
            series.add(new FundChartSeriesVO(
                    text(seriesNode, "name", null),
                    toSeriesValues(seriesNode.path("data"))
            ));
        }
        return new FundChartBlockVO(toStringList(node.path("categories")), series);
    }

    private List<FundScalePointVO> parseScaleTrend(String script) {
        JsonNode node = parseOptionalJsonAssignment(script, "Data_fluctuationScale");
        if (node == null || node.isMissingNode()) {
            return List.of();
        }

        List<String> categories = toStringList(node.path("categories"));
        List<FundScalePointVO> result = new ArrayList<>();
        JsonNode series = node.path("series");
        for (int index = 0; index < categories.size() && index < series.size(); index++) {
            JsonNode item = series.get(index);
            result.add(new FundScalePointVO(
                    categories.get(index),
                    parseDecimalNode(item.path("y")),
                    text(item, "mom", null)
            ));
        }
        return result;
    }

    private List<FundPeerReferenceVO> parsePeerReferences(String script) {
        JsonNode node = parseOptionalJsonAssignment(script, "swithSameType");
        if (node == null || !node.isArray()) {
            return List.of();
        }

        JsonNode selected = null;
        for (JsonNode candidate : node) {
            if (candidate.isArray() && !candidate.isEmpty()) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return List.of();
        }

        List<FundPeerReferenceVO> result = new ArrayList<>();
        for (JsonNode item : selected) {
            String raw = item.asText("");
            String[] pieces = raw.split("_", 3);
            if (pieces.length != 3) {
                continue;
            }
            result.add(new FundPeerReferenceVO(
                    pieces[0],
                    pieces[1],
                    parseDecimal(pieces[2])
            ));
        }
        return result;
    }

    private JsonNode parseJsonAssignment(String script, String variableName) {
        String raw = extractAssignment(script, variableName);
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("Missing script variable: " + variableName);
        }
        try {
            return jsonMapper.readTree(raw);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse script variable: " + variableName, ex);
        }
    }

    private JsonNode parseOptionalJsonAssignment(String script, String variableName) {
        String raw = extractAssignment(script, variableName);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return jsonMapper.readTree(raw);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse script variable: " + variableName, ex);
        }
    }

    private String extractAssignment(String script, String variableName) {
        Pattern pattern = Pattern.compile(
                "var\\s+" + Pattern.quote(variableName) + "\\s*=\\s*(.*?);(?=\\s*/\\*|\\s*var\\s+\\w+\\s*=|\\s*$)",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(script == null ? "" : script);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractQuotedValue(String script, String variableName) {
        Pattern pattern = Pattern.compile(
                "var\\s+" + Pattern.quote(variableName) + "\\s*=\\s*\"(.*?)\";",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(script == null ? "" : script);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractContent(String body) {
        Pattern pattern = Pattern.compile("content:\"(.*)\",arryear:", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new IllegalStateException("Holdings response does not contain HTML content");
        }
        return matcher.group(1);
    }

    private String unescapeContent(String content) {
        return content
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", "")
                .replace("\\r", "");
    }

    private boolean matchesQuarter(Element box, int year, int quarter) {
        String reportDateText = box.select("h4 .right font").text();
        if (StringUtils.hasText(reportDateText)) {
            try {
                LocalDate reportDate = LocalDate.parse(reportDateText.trim(), REPORT_DATE_FORMATTER);
                return reportDate.getYear() == year && quarterOf(reportDate) == quarter;
            } catch (Exception ignored) {
            }
        }

        String title = box.select("h4 .left").text();
        return title.contains(String.valueOf(year)) && title.contains(String.valueOf(quarter));
    }

    private List<String> toStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private List<BigDecimal> toNumberList(JsonNode node) {
        List<BigDecimal> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                result.add(parseDecimalNode(item));
            }
        }
        return result;
    }

    private List<BigDecimal> toSeriesValues(JsonNode node) {
        List<BigDecimal> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isObject() && item.has("y")) {
                    result.add(parseDecimalNode(item.path("y")));
                } else {
                    result.add(parseDecimalNode(item));
                }
            }
        }
        return result;
    }

    private BigDecimal parseNumberText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDecimal(value.replace("%", "").replace(",", "").trim());
    }

    private BigDecimal parseDecimalNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return parseDecimal(node.asText());
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("null".equalsIgnoreCase(normalized) || "--".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), ESTIMATE_TIME_FORMATTER);
    }

    private LocalDate toDate(long timestampMillis) {
        return Instant.ofEpochMilli(timestampMillis).atZone(ZONE_ID).toLocalDate();
    }

    private int quarterOf(LocalDate date) {
        return ((date.getMonthValue() - 1) / 3) + 1;
    }

    private String text(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText();
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

record EastmoneySearchItem(
        String fundCode,
        String fundName,
        String fundType,
        String managementCompany
) {
}

record EastmoneyEstimatePayload(
        String fundCode,
        String fundName,
        BigDecimal estimateNav,
        BigDecimal estimateGrowthRate,
        LocalDateTime estimateTime
) {
}

record EastmoneyNavPoint(
        LocalDate date,
        BigDecimal unitNav,
        BigDecimal accumulatedNav,
        BigDecimal dailyGrowthRate
) {
}

record EastmoneyFundDetailPayload(
        String fundCode,
        String fundName,
        BigDecimal sourceRate,
        BigDecimal currentRate,
        BigDecimal minPurchaseAmount,
        List<FundReturnStatVO> returnStats,
        FundPerformanceRadarVO performanceRadar,
        List<FundManagerCardVO> managers,
        FundChartBlockVO assetAllocation,
        FundChartBlockVO holderStructure,
        List<FundScalePointVO> scaleTrend,
        List<FundPeerReferenceVO> sameTypeReferences,
        List<EastmoneyNavPoint> navPoints
) {
}

record EastmoneyHoldingPayload(
        String fundCode,
        Integer year,
        Integer quarter,
        LocalDate reportDate,
        List<FundHoldingItemVO> items
) {
}
