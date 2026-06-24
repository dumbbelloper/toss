package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 체결 결과. 체결이 없으면 {@code filledQuantity=0}, 나머지는 null.
 * 금액은 native currency 기준.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderExecution(
        BigDecimal filledQuantity,
        BigDecimal averageFilledPrice,
        BigDecimal filledAmount,
        BigDecimal commission,
        BigDecimal tax,
        String filledAt,
        String settlementDate
) {
}
