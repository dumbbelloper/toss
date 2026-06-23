package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 캔들 차트 페이징 응답.
 *
 * @param candles    캔들 목록 (과거→현재 순)
 * @param nextBefore 다음 페이지 조회용 {@code before} 값. 마지막 페이지면 null
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CandlePageResponse(
        List<Candle> candles,
        String nextBefore
) {
}
