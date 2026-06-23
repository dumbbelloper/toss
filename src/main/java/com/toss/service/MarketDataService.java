package com.toss.service;

import com.toss.client.MarketDataClient;
import com.toss.client.dto.OrderbookResponse;
import com.toss.client.dto.PriceResponse;
import com.toss.common.TossApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Supplier;

/**
 * 시세 조회 서비스. 선언형 클라이언트를 감싸 응답 envelope 를 벗기고,
 * HTTP 에러를 {@link TossApiException} 으로 변환한다.
 */
@Service
public class MarketDataService {

    private final MarketDataClient client;

    public MarketDataService(MarketDataClient client) {
        this.client = client;
    }

    /** 현재가 조회 (복수 종목). */
    public List<PriceResponse> prices(String... symbols) {
        return execute(() -> client.getPrices(String.join(",", symbols)).result());
    }

    /** 호가 조회 (단일 종목). */
    public OrderbookResponse orderbook(String symbol) {
        return execute(() -> client.getOrderbook(symbol).result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
