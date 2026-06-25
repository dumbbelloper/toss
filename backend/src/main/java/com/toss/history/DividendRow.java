package com.toss.history;

import java.time.LocalDate;

/** 실분배금 한 건(1주당, 상장 통화 기준). Yahoo events=div 에서 추출. */
public record DividendRow(LocalDate exDate, double amount) {
}
