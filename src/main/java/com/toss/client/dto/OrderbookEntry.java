package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 호가 한 단계 (가격 + 잔량).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderbookEntry(
        BigDecimal price,
        BigDecimal volume
) {
}
