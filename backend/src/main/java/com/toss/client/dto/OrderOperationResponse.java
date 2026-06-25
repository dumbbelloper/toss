package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 정정/취소 응답. {@link #orderId()} 는 <b>새로 발급된</b> 주문 식별자로 원주문과 다르다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderOperationResponse(String orderId) {
}
