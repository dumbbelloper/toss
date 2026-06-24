package com.toss.ratelimit;

import org.springframework.http.HttpMethod;

/**
 * 토스증권 Rate Limit 그룹과 초당 허용 요청 수(TPS).
 * <p>한도는 {@code 클라이언트 × API 그룹} 단위로 적용된다. 수치는 운영 상황에 따라
 * 사전 공지 없이 조정될 수 있으며, 실제 허용치는 응답 헤더 {@code X-RateLimit-Limit} 로 확인한다.
 * <p>주의: {@code ORDER}/{@code ORDER_INFO} 는 09:00~09:10 KST 피크 구간에 3 TPS 로 강화되지만,
 * 여기서는 정적 한도만 두고 피크 초과 시 429 처리(재시도)에 의존한다.
 */
public enum RateLimitGroup {

    AUTH(5),
    ACCOUNT(1),
    ASSET(5),
    STOCK(5),
    MARKET_INFO(3),
    MARKET_DATA(10),
    MARKET_DATA_CHART(5),
    ORDER(6),
    ORDER_HISTORY(5),
    ORDER_INFO(6);

    private final int permitsPerSecond;

    RateLimitGroup(int permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    public int permitsPerSecond() {
        return permitsPerSecond;
    }

    /** 요청 메서드 + 경로로부터 해당 Rate Limit 그룹을 판별한다. */
    public static RateLimitGroup resolve(HttpMethod method, String path) {
        if (path.startsWith("/oauth2/token")) {
            return AUTH;
        }
        if (path.equals("/api/v1/candles")) {
            return MARKET_DATA_CHART;
        }
        if (path.equals("/api/v1/orderbook") || path.equals("/api/v1/prices")
                || path.equals("/api/v1/trades") || path.equals("/api/v1/price-limits")) {
            return MARKET_DATA;
        }
        if (path.startsWith("/api/v1/stocks")) {
            return STOCK;
        }
        if (path.equals("/api/v1/exchange-rate") || path.startsWith("/api/v1/market-calendar")) {
            return MARKET_INFO;
        }
        if (path.equals("/api/v1/accounts")) {
            return ACCOUNT;
        }
        if (path.equals("/api/v1/holdings")) {
            return ASSET;
        }
        if (path.equals("/api/v1/buying-power") || path.equals("/api/v1/sellable-quantity")
                || path.equals("/api/v1/commissions")) {
            return ORDER_INFO;
        }
        if (path.startsWith("/api/v1/orders")) {
            // 생성(/orders) · 정정(/orders/{id}/modify) · 취소(/orders/{id}/cancel) = POST → ORDER
            // 목록(/orders) · 상세(/orders/{id}) = GET → ORDER_HISTORY
            return HttpMethod.POST.equals(method) ? ORDER : ORDER_HISTORY;
        }
        // 미지의 경로는 가장 보수적인(=가장 높은 빈도가 허용되는) 시세 그룹으로 폴백
        return MARKET_DATA;
    }
}
