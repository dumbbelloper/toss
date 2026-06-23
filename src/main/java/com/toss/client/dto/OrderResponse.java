package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 주문 생성 응답. 정정/취소 시 {@link #orderId()} 를 사용한다.
 *
 * @param orderId       서버 생성 주문 식별자
 * @param clientOrderId 요청 시 전달한 멱등성 키 (미전달 시 null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderResponse(String orderId, String clientOrderId) {
}
