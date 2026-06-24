package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 판매 가능 수량 ({@code GET /api/v1/sellable-quantity}).
 * KR: 정수, US: 소수 포함 가능.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellableQuantityResponse(BigDecimal sellableQuantity) {
}
