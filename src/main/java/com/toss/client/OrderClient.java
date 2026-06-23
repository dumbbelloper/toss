package com.toss.client;

import com.toss.client.dto.Order;
import com.toss.client.dto.OrderCreateRequest;
import com.toss.client.dto.OrderModifyRequest;
import com.toss.client.dto.OrderOperationResponse;
import com.toss.client.dto.OrderQueryStatus;
import com.toss.client.dto.OrderResponse;
import com.toss.client.dto.PaginatedOrderResponse;
import com.toss.client.dto.TossEnvelope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * 주문 생성·정정·취소·조회 API ({@code X-Tossinvest-Account} 헤더 필요 — 인터셉터가 주입).
 */
@HttpExchange(url = "/api/v1", accept = "application/json", contentType = "application/json")
public interface OrderClient {

    /** 주문 생성 (지정가·시장가 / 수량·금액 기반). */
    @PostExchange("/orders")
    TossEnvelope<OrderResponse> createOrder(@RequestBody OrderCreateRequest request);

    /** 주문 정정 — 새 orderId 발급. */
    @PostExchange("/orders/{orderId}/modify")
    TossEnvelope<OrderOperationResponse> modifyOrder(@PathVariable String orderId,
                                                     @RequestBody OrderModifyRequest request);

    /** 주문 취소 — 새 orderId 발급. 본문은 빈 객체. */
    @PostExchange("/orders/{orderId}/cancel")
    TossEnvelope<OrderOperationResponse> cancelOrder(@PathVariable String orderId,
                                                     @RequestBody Map<String, Object> body);

    /** 주문 목록 조회 (대기중/종료). */
    @GetExchange("/orders")
    TossEnvelope<PaginatedOrderResponse> getOrders(@RequestParam("status") OrderQueryStatus status,
                                                   @RequestParam(name = "symbol", required = false) String symbol,
                                                   @RequestParam(name = "from", required = false) String from,
                                                   @RequestParam(name = "to", required = false) String to,
                                                   @RequestParam(name = "cursor", required = false) String cursor,
                                                   @RequestParam(name = "limit", required = false) Integer limit);

    /** 주문 상세 조회 (모든 상태). */
    @GetExchange("/orders/{orderId}")
    TossEnvelope<Order> getOrder(@PathVariable String orderId);
}
