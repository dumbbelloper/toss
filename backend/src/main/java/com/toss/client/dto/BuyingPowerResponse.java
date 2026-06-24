package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 매수 가능 금액 ({@code GET /api/v1/buying-power}).
 *
 * @param currency        통화
 * @param cashBuyingPower 현금 기반 매수 가능 금액 (미수 미발생 기준). KRW: 정수, USD: 소수 포함
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BuyingPowerResponse(
        Currency currency,
        BigDecimal cashBuyingPower
) {
}
