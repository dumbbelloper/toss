package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 주문 상세 ({@code GET /api/v1/orders/{orderId}} 및 목록 항목).
 * <p>{@code side}/{@code orderType}/{@code timeInForce}/{@code status} 는 String 으로 둔다 —
 * 스펙이 "클라이언트는 unknown code 를 허용하도록 구현"을 요구하므로, enum 강제 바인딩으로 인한
 * 역직렬화 실패를 피한다. (참고 값: status ∈ PENDING/PARTIAL_FILLED/FILLED/CANCELED/REJECTED 등)
 *
 * @param price       주문 가격 (native currency). MARKET 주문 시 null
 * @param orderAmount 주문 금액 (USD). 금액 기반 US 시장가 매수에만, 그 외 null
 * @param orderedAt   주문 시간 (ISO-8601, KST)
 * @param canceledAt  취소 시간 (없으면 null)
 * @param execution   체결 결과
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Order(
        String orderId,
        String symbol,
        String side,
        String orderType,
        String timeInForce,
        String status,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal orderAmount,
        Currency currency,
        String orderedAt,
        String canceledAt,
        OrderExecution execution
) {
}
