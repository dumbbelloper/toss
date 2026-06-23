package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 캔들 (OHLCV). {@code timestamp} 는 봉 시작 시각 (ISO-8601 문자열).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Candle(
        String timestamp,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        Currency currency
) {
}
