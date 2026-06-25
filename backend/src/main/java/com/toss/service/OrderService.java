package com.toss.service;

import com.toss.client.OrderClient;
import com.toss.client.dto.OrderCreateRequest;
import com.toss.client.dto.OrderModifyRequest;
import com.toss.client.dto.OrderOperationResponse;
import com.toss.client.dto.OrderResponse;
import com.toss.client.dto.Side;
import com.toss.common.TossApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 주문 생성·정정·취소 (쓰기). <b>자동 재시도하지 않는다</b> — 일시 오류라도 주문이 서버에서
 * 접수됐을 수 있어 자동 재시도는 중복 주문 위험이 있다. 대신 모든 생성 요청에 멱등성 키
 * ({@code clientOrderId}) 를 자동 부여해, 호출 측이 안전하게 수동 재시도할 수 있게 한다
 * (동일 키 재요청은 10분간 같은 주문 결과를 반환).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderClient client;

    public OrderService(OrderClient client) {
        this.client = client;
    }

    /** 주문 생성. clientOrderId 미지정 시 멱등성 키를 자동 부여한다. */
    public OrderResponse place(OrderCreateRequest request) {
        OrderCreateRequest finalRequest = request.clientOrderId() == null
                ? request.withClientOrderId(newIdempotencyKey())
                : request;
        log.info("주문 생성: symbol={} side={} type={} clientOrderId={}",
                finalRequest.symbol(), finalRequest.side(), finalRequest.orderType(), finalRequest.clientOrderId());
        return execute(() -> client.createOrder(finalRequest).result());
    }

    /** 지정가 매수. */
    public OrderResponse buyLimit(String symbol, BigDecimal quantity, BigDecimal price) {
        return place(OrderCreateRequest.limit(symbol, Side.BUY, quantity, price));
    }

    /** 지정가 매도. */
    public OrderResponse sellLimit(String symbol, BigDecimal quantity, BigDecimal price) {
        return place(OrderCreateRequest.limit(symbol, Side.SELL, quantity, price));
    }

    /** 시장가 매수 (수량 기반). */
    public OrderResponse buyMarket(String symbol, BigDecimal quantity) {
        return place(OrderCreateRequest.market(symbol, Side.BUY, quantity));
    }

    /** 시장가 매도 (수량 기반). */
    public OrderResponse sellMarket(String symbol, BigDecimal quantity) {
        return place(OrderCreateRequest.market(symbol, Side.SELL, quantity));
    }

    /** 주문 정정. 새 orderId 를 반환한다. */
    public OrderOperationResponse modify(String orderId, OrderModifyRequest request) {
        log.info("주문 정정: orderId={} type={}", orderId, request.orderType());
        return execute(() -> client.modifyOrder(orderId, request).result());
    }

    /** 주문 취소. 새 orderId 를 반환한다. */
    public OrderOperationResponse cancel(String orderId) {
        log.info("주문 취소: orderId={}", orderId);
        return execute(() -> client.cancelOrder(orderId, Map.of()).result());
    }

    /** 멱등성 키 (UUID, 36자, 패턴 [a-zA-Z0-9-_] 충족). */
    private static String newIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
