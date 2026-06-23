package com.toss.client;

import com.toss.client.dto.OrderbookResponse;
import com.toss.client.dto.PriceResponse;
import com.toss.client.dto.TossEnvelope;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 시세 조회 API 선언형 클라이언트 ({@code @HttpExchange}).
 * <p>토큰만으로 호출 가능(계좌 헤더 불필요).
 */
@HttpExchange(url = "/api/v1", accept = "application/json")
public interface MarketDataClient {

    /** 현재가 조회. 복수 종목은 콤마로 구분 (예: {@code 005930,000660}). */
    @GetExchange("/prices")
    TossEnvelope<List<PriceResponse>> getPrices(@RequestParam("symbols") String symbols);

    /** 호가 조회. */
    @GetExchange("/orderbook")
    TossEnvelope<OrderbookResponse> getOrderbook(@RequestParam("symbol") String symbol);
}
