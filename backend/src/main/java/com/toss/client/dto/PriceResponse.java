package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 현재가 조회 결과 ({@code GET /api/v1/prices}).
 *
 * @param symbol    종목 심볼
 * @param timestamp 데이터 시각 (ISO-8601, KST). 체결 미발생 등으로 없으면 null
 * @param lastPrice 현재가 (native currency)
 * @param currency  통화
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceResponse(
        String symbol,
        String timestamp,
        BigDecimal lastPrice,
        Currency currency
) {
}
