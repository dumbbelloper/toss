package com.toss.history;

import java.time.LocalDate;

/** 백테스트용 일별 가격 한 점. close=원시(가격수익), adjClose=조정(총수익/배당재투자). */
public record PriceRow(LocalDate date, double close, double adjClose) {
}
