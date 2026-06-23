package com.toss.service;

import com.toss.client.MarketDataClient;
import com.toss.client.dto.Currency;
import com.toss.client.dto.PriceResponse;
import com.toss.client.dto.TossEnvelope;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code @Retryable} 가 일시 에러(429/5xx)에만 재시도하는지 검증한다.
 * 지연은 {@code toss.retry.delay-ms=1} 로 줄여 빠르게 수행.
 */
@SpringJUnitConfig(MarketDataServiceRetryTest.Config.class)
@TestPropertySource(properties = {
        "toss.retry.max-retries=3",
        "toss.retry.delay-ms=1",
        "toss.retry.multiplier=1.0",
        "toss.retry.jitter-ms=0"
})
class MarketDataServiceRetryTest {

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    static class Config {
        @Bean
        MarketDataClient marketDataClient() {
            return mock(MarketDataClient.class);
        }

        @Bean
        MarketDataService marketDataService(MarketDataClient client) {
            return new MarketDataService(client);
        }
    }

    @Autowired
    MarketDataService service;
    @Autowired
    MarketDataClient client;

    @BeforeEach
    void resetMock() {
        // 컨텍스트가 캐시되어 mock 이 테스트 간 공유되므로 호출 횟수/스텁을 초기화한다.
        Mockito.reset(client);
    }

    @Test
    void retriesTransientErrorThenSucceeds() {
        TossEnvelope<List<PriceResponse>> ok = new TossEnvelope<>(
                List.of(new PriceResponse("005930", null, new BigDecimal("72000"), Currency.KRW)));
        when(client.getPrices(any()))
                .thenThrow(transient429())
                .thenThrow(transient429())
                .thenReturn(ok);

        List<PriceResponse> prices = service.prices("005930");

        assertThat(prices).hasSize(1);
        verify(client, times(3)).getPrices(any()); // 2 실패 + 1 성공
    }

    @Test
    void doesNotRetryNonTransientError() {
        when(client.getPrices(any()))
                .thenThrow(new TossApiException(HttpStatus.NOT_FOUND, "stock-not-found", "없음", null, null));

        assertThatThrownBy(() -> service.prices("ZZZ"))
                .isInstanceOf(TossApiException.class)
                .isNotInstanceOf(TossTransientException.class);
        verify(client, times(1)).getPrices(any()); // 재시도 없음
    }

    private static TossTransientException transient429() {
        return new TossTransientException(HttpStatus.TOO_MANY_REQUESTS, "rate-limit-exceeded",
                "too many", null, null);
    }
}
