package com.toss.backtest;

import java.util.List;

/**
 * 백테스트 결과. 수익률은 소수비율(0.21 = +21%).
 *
 * @param symbol         종목
 * @param strategy       전략명
 * @param params         사용한 파라미터 설명
 * @param bars           사용한 봉 개수
 * @param initialCapital 초기 자본
 * @param finalEquity    최종 평가금액
 * @param totalReturn    전략 총수익률
 * @param buyHoldReturn  같은 구간 매수후보유 수익률 (벤치마크)
 * @param maxDrawdown    최대 낙폭 (MDD, 양수 비율)
 * @param trades         완료된 매매 횟수 (진입→청산)
 * @param winRate        승률 (이익 청산 / 완료 매매)
 * @param equity         자본 곡선 (차트용)
 */
public record BacktestResult(
        String symbol,
        String strategy,
        String params,
        int bars,
        double initialCapital,
        double finalEquity,
        double totalReturn,
        double buyHoldReturn,
        double maxDrawdown,
        int trades,
        double winRate,
        List<EquityPoint> equity
) {

    /** 자본 곡선의 한 점. time: 'YYYY-MM-DD'. */
    public record EquityPoint(String time, double value) {
    }
}
