package com.toss.service;

import com.toss.client.OrderInfoClient;
import com.toss.client.dto.BuyingPowerResponse;
import com.toss.client.dto.Commission;
import com.toss.client.dto.Currency;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * 거래 가능 정보(매수 가능 금액·판매 가능 수량·수수료) 조회. 읽기 전용 → 재시도.
 */
@Service
@Retryable(
        includes = TossTransientException.class,
        maxRetriesString = "${toss.retry.max-retries:3}",
        delayString = "${toss.retry.delay-ms:1000}",
        multiplierString = "${toss.retry.multiplier:2.0}",
        maxDelayString = "${toss.retry.max-delay-ms:8000}",
        jitterString = "${toss.retry.jitter-ms:250}")
public class OrderInfoService {

    private final OrderInfoClient client;

    public OrderInfoService(OrderInfoClient client) {
        this.client = client;
    }

    /** 매수 가능 금액 (현금 기반). */
    public BigDecimal buyingPower(Currency currency) {
        return execute(() -> client.getBuyingPower(currency).result()).cashBuyingPower();
    }

    /** 매수 가능 금액 전체 응답. */
    public BuyingPowerResponse buyingPowerDetail(Currency currency) {
        return execute(() -> client.getBuyingPower(currency).result());
    }

    /** 판매 가능 수량. */
    public BigDecimal sellableQuantity(String symbol) {
        return execute(() -> client.getSellableQuantity(symbol).result()).sellableQuantity();
    }

    /** 매매 수수료 목록 (시장별). */
    public List<Commission> commissions() {
        return execute(() -> client.getCommissions().result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
