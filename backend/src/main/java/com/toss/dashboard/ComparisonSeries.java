package com.toss.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * 보유 종목별 "매수가 대비 성과" 시계열. 각 점의 value 는 종가를 매수평균가로 정규화한 값
 * (100 = 매수평균가 = 손익분기). 100 위 = 평가이익, 아래 = 평가손실.
 *
 * @param symbol               종목 심볼
 * @param name                 종목명
 * @param averagePurchasePrice 매수 평균가 (cost basis)
 * @param purchaseDate         최초 매수 체결일 (주문이력 기반, 없으면 null)
 * @param points               정규화 시계열
 */
public record ComparisonSeries(
        String symbol,
        String name,
        BigDecimal averagePurchasePrice,
        String purchaseDate,
        List<Point> points
) {

    /** time: 'YYYY-MM-DD', value: 매수가 대비 % (100 기준). */
    public record Point(String time, double value) {
    }
}
