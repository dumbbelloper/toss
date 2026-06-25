package com.toss.history;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 일별 환율 한 점. */
public record FxRate(LocalDate date, BigDecimal rate) {
}
