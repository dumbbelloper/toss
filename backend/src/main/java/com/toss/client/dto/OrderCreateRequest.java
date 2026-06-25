package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * 주문 생성 요청 ({@code POST /api/v1/orders}). null 필드는 직렬화 시 생략된다.
 * <p>두 가지 방식:
 * <ul>
 *   <li><b>수량 기반</b>: {@code quantity} 지정 (KR·US, 정수 수량). 지정가는 {@code price} 필수.</li>
 *   <li><b>금액 기반</b>: {@code orderAmount} 지정 (US MARKET 전용, 소수 가능, 정규장만).</li>
 * </ul>
 * 금액/수량은 API 가 문자열(decimal)을 요구하므로 String 으로 보관한다.
 * 정적 팩토리로 생성을 권장한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCreateRequest(
        String clientOrderId,
        String symbol,
        Side side,
        OrderType orderType,
        TimeInForce timeInForce,
        String quantity,
        String price,
        String orderAmount,
        Boolean confirmHighValueOrder
) {

    /** 지정가 주문 (수량 기반). tif 미지정 시 DAY. */
    public static OrderCreateRequest limit(String symbol, Side side, BigDecimal quantity, BigDecimal price,
                                           TimeInForce timeInForce) {
        requirePositive(quantity, "quantity");
        requirePositive(price, "price");
        return new OrderCreateRequest(null, requireSymbol(symbol), requireSide(side), OrderType.LIMIT,
                timeInForce, plain(quantity), plain(price), null, null);
    }

    /** 지정가 주문 (당일 유효). */
    public static OrderCreateRequest limit(String symbol, Side side, BigDecimal quantity, BigDecimal price) {
        return limit(symbol, side, quantity, price, TimeInForce.DAY);
    }

    /** 시장가 주문 (수량 기반). */
    public static OrderCreateRequest market(String symbol, Side side, BigDecimal quantity) {
        requirePositive(quantity, "quantity");
        return new OrderCreateRequest(null, requireSymbol(symbol), requireSide(side), OrderType.MARKET,
                null, plain(quantity), null, null, null);
    }

    /** 금액 기반 시장가 주문 (US 전용, 정규장만). */
    public static OrderCreateRequest amountMarket(String symbol, Side side, BigDecimal orderAmount) {
        requirePositive(orderAmount, "orderAmount");
        return new OrderCreateRequest(null, requireSymbol(symbol), requireSide(side), OrderType.MARKET,
                null, null, null, plain(orderAmount), null);
    }

    /** 멱등성 키를 지정한 복사본. */
    public OrderCreateRequest withClientOrderId(String id) {
        return new OrderCreateRequest(id, symbol, side, orderType, timeInForce, quantity, price, orderAmount,
                confirmHighValueOrder);
    }

    /** 고액(1억↑) 주문 확인 플래그를 지정한 복사본. */
    public OrderCreateRequest withConfirmHighValueOrder(boolean confirm) {
        return new OrderCreateRequest(clientOrderId, symbol, side, orderType, timeInForce, quantity, price,
                orderAmount, confirm);
    }

    private static String plain(BigDecimal v) {
        return v.toPlainString();
    }

    private static void requirePositive(BigDecimal v, String field) {
        if (v == null || v.signum() <= 0) {
            throw new IllegalArgumentException(field + " 는 양수여야 합니다: " + v);
        }
    }

    private static String requireSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol 은 필수입니다.");
        }
        return symbol;
    }

    private static Side requireSide(Side side) {
        if (side == null) {
            throw new IllegalArgumentException("side 는 필수입니다.");
        }
        return side;
    }
}
