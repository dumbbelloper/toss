package com.toss.service;

import com.toss.client.MarketInfoClient;
import com.toss.client.dto.Currency;
import com.toss.client.dto.ExchangeRateResponse;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * 시장 정보 조회 (환율 등). 읽기 전용 → 재시도.
 */
@Service
@Retryable(
        includes = TossTransientException.class,
        maxRetriesString = "${toss.retry.max-retries:3}",
        delayString = "${toss.retry.delay-ms:1000}",
        multiplierString = "${toss.retry.multiplier:2.0}",
        maxDelayString = "${toss.retry.max-delay-ms:8000}",
        jitterString = "${toss.retry.jitter-ms:250}")
public class MarketInfoService {

    private final MarketInfoClient client;

    public MarketInfoService(MarketInfoClient client) {
        this.client = client;
    }

    /** 환율 조회 (1 base = ? quote). */
    public ExchangeRateResponse exchangeRate(Currency base, Currency quote) {
        return execute(() -> client.getExchangeRate(base, quote).result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
