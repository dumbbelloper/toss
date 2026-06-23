package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 통화별 합산 금액. 각 통화 필드는 해당 통화로 거래된 종목의 합만 포함한다
 * (환율 환산을 통한 통화 간 합산은 미포함).
 *
 * @param krw 원화 합산 (국내 종목 없으면 0)
 * @param usd 달러 합산 (해외 종목 없으면 null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Price(BigDecimal krw, BigDecimal usd) {
}
