package com.toss.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class RateLimitGroupTest {

    @Test
    void resolvesMarketDataEndpoints() {
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/prices")).isEqualTo(RateLimitGroup.MARKET_DATA);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/orderbook")).isEqualTo(RateLimitGroup.MARKET_DATA);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/candles")).isEqualTo(RateLimitGroup.MARKET_DATA_CHART);
    }

    @Test
    void resolvesOrderEndpointsByMethod() {
        // 생성/정정/취소(POST) → ORDER, 목록/상세(GET) → ORDER_HISTORY
        assertThat(RateLimitGroup.resolve(POST, "/api/v1/orders")).isEqualTo(RateLimitGroup.ORDER);
        assertThat(RateLimitGroup.resolve(POST, "/api/v1/orders/abc/cancel")).isEqualTo(RateLimitGroup.ORDER);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/orders")).isEqualTo(RateLimitGroup.ORDER_HISTORY);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/orders/abc")).isEqualTo(RateLimitGroup.ORDER_HISTORY);
    }

    @Test
    void resolvesAccountAndOrderInfoEndpoints() {
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/accounts")).isEqualTo(RateLimitGroup.ACCOUNT);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/holdings")).isEqualTo(RateLimitGroup.ASSET);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/buying-power")).isEqualTo(RateLimitGroup.ORDER_INFO);
        assertThat(RateLimitGroup.resolve(GET, "/api/v1/stocks/005930/warnings")).isEqualTo(RateLimitGroup.STOCK);
    }
}
