package com.toss.service;

import com.toss.client.OrderClient;
import com.toss.client.dto.Order;
import com.toss.client.dto.OrderQueryStatus;
import com.toss.client.dto.PaginatedOrderResponse;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * 주문 조회 (읽기). 일시 에러(429/5xx)에 재시도한다.
 */
@Service
@Retryable(
        includes = TossTransientException.class,
        maxRetriesString = "${toss.retry.max-retries:3}",
        delayString = "${toss.retry.delay-ms:1000}",
        multiplierString = "${toss.retry.multiplier:2.0}",
        maxDelayString = "${toss.retry.max-delay-ms:8000}",
        jitterString = "${toss.retry.jitter-ms:250}")
public class OrderQueryService {

    private final OrderClient client;

    public OrderQueryService(OrderClient client) {
        this.client = client;
    }

    /** 대기중 주문 전체. */
    public PaginatedOrderResponse openOrders() {
        return execute(() -> client.getOrders(OrderQueryStatus.OPEN, null, null, null, null, null).result());
    }

    /** 종목별 대기중 주문. */
    public PaginatedOrderResponse openOrders(String symbol) {
        return execute(() -> client.getOrders(OrderQueryStatus.OPEN, symbol, null, null, null, null).result());
    }

    /** 종료된 주문 (페이징). */
    public PaginatedOrderResponse closedOrders(String cursor, Integer limit) {
        return execute(() -> client.getOrders(OrderQueryStatus.CLOSED, null, null, null, cursor, limit).result());
    }

    /** 주문 상세. */
    public Order order(String orderId) {
        return execute(() -> client.getOrder(orderId).result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
