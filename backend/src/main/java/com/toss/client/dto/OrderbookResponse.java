package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 호가 조회 결과 ({@code GET /api/v1/orderbook}).
 *
 * @param timestamp 데이터 시각 (ISO-8601, KST). 없으면 null
 * @param currency  통화
 * @param asks      매도 호가 (낮은 가격 우선)
 * @param bids      매수 호가 (높은 가격 우선)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderbookResponse(
        String timestamp,
        Currency currency,
        List<OrderbookEntry> asks,
        List<OrderbookEntry> bids
) {
}
