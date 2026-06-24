package com.toss.service;

import com.toss.client.MarketDataClient;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.OrderbookResponse;
import com.toss.client.dto.PriceResponse;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Supplier;

/**
 * 시세 조회 서비스. 선언형 클라이언트를 감싸 응답 envelope 를 벗기고,
 * HTTP 에러를 {@link TossApiException} 으로 변환한다.
 * <p>429/5xx({@link TossTransientException}) 는 지수 백오프로 재시도한다(설정값은
 * {@code toss.retry.*} 로 외부화). 4xx 는 재시도하지 않는다.
 */
@Service
@Retryable(
        includes = TossTransientException.class,
        maxRetriesString = "${toss.retry.max-retries:3}",
        delayString = "${toss.retry.delay-ms:1000}",
        multiplierString = "${toss.retry.multiplier:2.0}",
        maxDelayString = "${toss.retry.max-delay-ms:8000}",
        jitterString = "${toss.retry.jitter-ms:250}")
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

    /** 캔들 조회 (수정주가 기준). count 최근 봉. */
    public CandlePageResponse candles(String symbol, CandleInterval interval, Integer count) {
        return execute(() -> client.getCandles(symbol, interval.code(), count, null, true).result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
