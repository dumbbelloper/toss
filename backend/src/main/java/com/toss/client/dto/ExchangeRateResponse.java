package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 환율 조회 결과 ({@code GET /api/v1/exchange-rate}).
 *
 * @param baseCurrency   기준 통화
 * @param quoteCurrency  표시 통화
 * @param rate           매수 환율 (1 base = ? quote)
 * @param midRate        매매기준율 (은행간 mid rate)
 * @param rateChangeType 등락 구분 (UP/DOWN/FLAT 등, unknown 허용 위해 String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateResponse(
        Currency baseCurrency,
        Currency quoteCurrency,
        BigDecimal rate,
        BigDecimal midRate,
        String rateChangeType
) {
}
