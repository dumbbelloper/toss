package com.toss.history;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance chart API 어댑터. 원시 close + 조정 close(분할+배당=총수익)를 함께 가져온다.
 * 교체형 — 다른 provider(Stooq/Tiingo)도 같은 DailyBar/FxRate 로 구현 가능.
 */
@Component
public class YahooHistoryProvider {

    private final RestClient yahoo;

    public YahooHistoryProvider(RestClient yahooRestClient) {
        this.yahoo = yahooRestClient;
    }

    /** 상장일~현재 일봉. 빈 응답이면 빈 목록. */
    public List<DailyBar> dailyHistory(String symbol) {
        JsonNode result = fetch(symbol);
        if (result == null) {
            return List.of();
        }
        ZoneId zone = exchangeZone(result);
        JsonNode ts = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").path(0);
        JsonNode adj = result.path("indicators").path("adjclose").path(0).path("adjclose");
        JsonNode open = quote.path("open"), high = quote.path("high"), low = quote.path("low"),
                close = quote.path("close"), vol = quote.path("volume");

        List<DailyBar> bars = new ArrayList<>(ts.size());
        for (int i = 0; i < ts.size(); i++) {
            BigDecimal c = dec(close, i);
            BigDecimal a = dec(adj, i);
            if (c == null || a == null) {
                continue; // 미완성/결측 봉 스킵
            }
            LocalDate date = Instant.ofEpochSecond(ts.get(i).asLong()).atZone(zone).toLocalDate();
            bars.add(new DailyBar(date, dec(open, i), dec(high, i), dec(low, i), c, a, lng(vol, i)));
        }
        return bars;
    }

    /** 환율 일별(close=환율). Yahoo 심볼 예: USDKRW = "KRW=X". */
    public List<FxRate> fxHistory(String yahooSymbol) {
        JsonNode result = fetch(yahooSymbol);
        if (result == null) {
            return List.of();
        }
        ZoneId zone = exchangeZone(result);
        JsonNode ts = result.path("timestamp");
        JsonNode close = result.path("indicators").path("quote").path(0).path("close");
        List<FxRate> rates = new ArrayList<>(ts.size());
        for (int i = 0; i < ts.size(); i++) {
            BigDecimal r = dec(close, i);
            if (r == null) {
                continue;
            }
            LocalDate date = Instant.ofEpochSecond(ts.get(i).asLong()).atZone(zone).toLocalDate();
            rates.add(new FxRate(date, r));
        }
        return rates;
    }

    private JsonNode fetch(String symbol) {
        JsonNode root = yahoo.get()
                .uri(b -> b.path("/v8/finance/chart/{s}")
                        .queryParam("period1", 0)
                        .queryParam("period2", Instant.now().getEpochSecond())
                        .queryParam("interval", "1d")
                        .queryParam("events", "div,splits")
                        .build(symbol))
                .retrieve()
                .body(JsonNode.class);
        if (root == null) {
            return null;
        }
        JsonNode result = root.path("chart").path("result");
        return result.isArray() && !result.isEmpty() ? result.get(0) : null;
    }

    private static ZoneId exchangeZone(JsonNode result) {
        String tz = result.path("meta").path("exchangeTimezoneName").asText("America/New_York");
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.of("America/New_York");
        }
    }

    private static BigDecimal dec(JsonNode arr, int i) {
        JsonNode n = arr.path(i);
        return n.isNumber() ? n.decimalValue() : null;
    }

    private static Long lng(JsonNode arr, int i) {
        JsonNode n = arr.path(i);
        return n.isNumber() ? n.asLong() : null;
    }
}
