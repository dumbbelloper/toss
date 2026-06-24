package com.toss.monitor;

import com.toss.client.dto.Currency;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 시세 스냅샷 (price_snapshot 테이블). 폴링 시점의 현재가를 이력으로 저장한다.
 */
@Table("price_snapshot")
public record PriceSnapshot(
        @Id Long id,
        String symbol,
        BigDecimal lastPrice,
        String currency,
        Instant ts,
        Instant fetchedAt
) {

    /** 신규 삽입용 (id=null). */
    public static PriceSnapshot of(String symbol, BigDecimal lastPrice, Currency currency,
                                   Instant ts, Instant fetchedAt) {
        return new PriceSnapshot(null, symbol, lastPrice, currency.name(), ts, fetchedAt);
    }
}
