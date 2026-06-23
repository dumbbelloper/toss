package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 주문 정정 요청 ({@code POST /api/v1/orders/{orderId}/modify}). null 필드는 생략.
 * KR 주식은 quantity 필수(양의 정수). LIMIT 은 price 필수.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderModifyRequest(
        OrderType orderType,
        String quantity,
        String price,
        Boolean confirmHighValueOrder
) {

    /** 지정가로 정정 (가격·수량 변경). */
    public static OrderModifyRequest limit(BigDecimal quantity, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("LIMIT 정정은 price 가 양수여야 합니다: " + price);
        }
        return new OrderModifyRequest(OrderType.LIMIT,
                quantity != null ? quantity.toPlainString() : null, price.toPlainString(), null);
    }

    /** 시장가로 정정 (수량 변경). */
    public static OrderModifyRequest market(BigDecimal quantity) {
        return new OrderModifyRequest(OrderType.MARKET,
                quantity != null ? quantity.toPlainString() : null, null, null);
    }

    public OrderModifyRequest withConfirmHighValueOrder(boolean confirm) {
        return new OrderModifyRequest(orderType, quantity, price, confirm);
    }
}
