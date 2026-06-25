package com.toss.history;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일별 시세 한 봉. close 는 분할조정 원시 종가(가격수익), adjClose 는 분할+배당 조정(총수익).
 */
public record DailyBar(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjClose,
        Long volume
) {
}
