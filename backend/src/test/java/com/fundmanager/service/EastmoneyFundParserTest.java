package com.fundmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EastmoneyFundParserTest {

    private final EastmoneyFundParser parser = new EastmoneyFundParser(new ObjectMapper());

    @Test
    void shouldParseSearchResponse() {
        String body = """
                {"ErrCode":0,"ErrMsg":"fromes","Datas":[{"CODE":"161725","NAME":"Fund A","CATEGORYDESC":"Fund","FundBaseInfo":{"FCODE":"161725","FTYPE":"Index","JJGS":"Manager Co","SHORTNAME":"Fund A"}}]}
                """;

        List<EastmoneySearchItem> items = parser.parseSearchResponse(body);

        assertEquals(1, items.size());
        assertEquals("161725", items.get(0).fundCode());
        assertEquals("Fund A", items.get(0).fundName());
        assertEquals("Index", items.get(0).fundType());
        assertEquals("Manager Co", items.get(0).managementCompany());
    }

    @Test
    void shouldParseEstimateResponse() {
        String body = """
                jsonpgz({"fundcode":"161725","name":"Fund A","jzrq":"2026-04-01","dwjz":"0.6474","gsz":"0.6500","gszzl":"0.40","gztime":"2026-04-02 15:00"});
                """;

        EastmoneyEstimatePayload payload = parser.parseEstimateResponse("161725", body);

        assertEquals("161725", payload.fundCode());
        assertEquals("Fund A", payload.fundName());
        assertEquals(new BigDecimal("0.6500"), payload.estimateNav());
        assertEquals(new BigDecimal("0.40"), payload.estimateGrowthRate());
        assertEquals(LocalDateTime.of(2026, 4, 2, 15, 0), payload.estimateTime());
    }

    @Test
    void shouldParseDetailScript() {
        String script = """
                var fS_name = "Fund A";
                var fS_code = "161725";
                var fund_sourceRate="1.00";
                var fund_Rate="0.10";
                var fund_minsg="10";
                var syl_1n="-21.32";
                var syl_6y="-17.47";
                var syl_3y="-8.8";
                var syl_1y="-5.54";
                var Data_netWorthTrend = [{"x":1711929600000,"y":1.23,"equityReturn":1.11,"unitMoney":""},{"x":1712016000000,"y":1.25,"equityReturn":1.63,"unitMoney":""}];
                var Data_ACWorthTrend = [[1711929600000,1.23],[1712016000000,1.25]];
                var Data_fluctuationScale = {"categories":["2025-03-31","2025-06-30"],"series":[{"y":334.81,"mom":"-5.47%"},{"y":295.72,"mom":"-11.68%"}]};
                var Data_holderStructure = {"series":[{"name":"Inst","data":[0.83,2.09]}],"categories":["2024-06-30","2024-12-31"]};
                var Data_assetAllocation = {"series":[{"name":"Stock","data":[94.72,94.55]}],"categories":["2025-03-31","2025-06-30"]};
                var Data_performanceEvaluation = {"avr":"77.75","categories":["A","B","C"],"data":[50.0,60.0,70.0]};
                var Data_currentFundManager = [{"id":"30379533","pic":"https://example.com/a.png","name":"Manager","star":5,"workTime":"8Y","fundSize":"574.41","power":{"avr":"83.34","categories":["A"],"data":[87.30]},"profit":{"categories":["Term","Avg","Index"],"series":[{"data":[{"y":78.8272},{"y":73.56},{"y":20.62}]}]}}];
                var swithSameType = [['020899_FundX_152.94','020900_FundY_152.43']];
                """;

        EastmoneyFundDetailPayload payload = parser.parseDetailScript(script);

        assertEquals("161725", payload.fundCode());
        assertEquals("Fund A", payload.fundName());
        assertEquals(new BigDecimal("0.10"), payload.currentRate());
        assertEquals(4, payload.returnStats().size());
        assertEquals("1M", payload.returnStats().get(0).label());
        assertEquals(2, payload.navPoints().size());
        assertEquals(LocalDate.of(2024, 4, 1), payload.navPoints().get(0).date());
        assertEquals(1, payload.managers().size());
        assertEquals(2, payload.sameTypeReferences().size());
        assertEquals("020899", payload.sameTypeReferences().get(0).fundCode());
    }

    @Test
    void shouldParseHoldingsResponse() {
        String body = """
                var apidata={ content:"<div class='box'><div class='boxitem'><h4 class='t'><label class='left'>Fund A 2025 Q4 Holdings</label><label class='right'><font class='px12'>2025-12-31</font></label></h4><table><tbody><tr><td>1</td><td>600519</td><td>Stock A</td><td>15.38%</td><td>482.29</td><td>664,201.11</td></tr></tbody></table></div></div>",arryear:[2025],curyear:2025};
                """;

        EastmoneyHoldingPayload payload = parser.parseHoldingsResponse("161725", 2025, 4, body);

        assertEquals(LocalDate.of(2025, 12, 31), payload.reportDate());
        assertEquals(1, payload.items().size());
        assertEquals("600519", payload.items().get(0).stockCode());
        assertEquals(new BigDecimal("15.38"), payload.items().get(0).navRatio());
        assertEquals(new BigDecimal("482.29"), payload.items().get(0).holdingShares());
        assertEquals(new BigDecimal("664201.11"), payload.items().get(0).holdingMarketValue());
    }
}
