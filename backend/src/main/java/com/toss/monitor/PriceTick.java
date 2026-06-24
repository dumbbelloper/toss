package com.toss.monitor;

import com.toss.client.dto.Currency;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 시세 변동 이벤트. 폴러가 발행하고 SSE·전략·알림이 구독한다.
 *
 * @param symbol    종목 심볼
 * @param lastPrice 현재가
 * @param currency  통화
 * @param timestamp 데이터 시각 (ISO-8601 문자열, 없으면 null)
 * @param fetchedAt 폴링 시각
 */
public record PriceTick(
        String symbol,
        BigDecimal lastPrice,
        Currency currency,
        String timestamp,
        Instant fetchedAt
) {
}
