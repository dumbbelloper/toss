package com.toss.backtest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BacktesterTest {

    private static String[] days(int n) {
        String[] t = new String[n];
        for (int i = 0; i < n; i++) {
            t[i] = String.format("2026-01-%02d", i + 1);
        }
        return t;
    }

    @Test
    void buyAndHoldEqualsBuyHoldReturnAndNeverTrades() {
        double[] c = {100, 110, 121};
        boolean[] pos = Backtester.buyAndHold(3);

        BacktestResult r = Backtester.simulate("X", BacktestStrategy.BUY_AND_HOLD, "", days(3), c, pos, 1000);

        assertThat(r.totalReturn()).isCloseTo(0.21, within(1e-9));
        assertThat(r.buyHoldReturn()).isCloseTo(0.21, within(1e-9));
        assertThat(r.finalEquity()).isCloseTo(1210.0, within(1e-6));
        assertThat(r.trades()).isZero();
    }

    @Test
    void simulateCountsCompletedWinningTrade() {
        double[] c = {100, 100, 105, 110};
        boolean[] pos = {false, true, true, false}; // 진입 100 → 청산 110

        BacktestResult r = Backtester.simulate("X", BacktestStrategy.SMA_CROSS, "", days(4), c, pos, 1000);

        assertThat(r.trades()).isEqualTo(1);
        assertThat(r.winRate()).isEqualTo(1.0);
        assertThat(r.totalReturn()).isCloseTo(0.10, within(1e-9));
    }

    @Test
    void maxDrawdownIsPeakToTrough() {
        List<BacktestResult.EquityPoint> eq = List.of(
                new BacktestResult.EquityPoint("a", 100),
                new BacktestResult.EquityPoint("b", 120),
                new BacktestResult.EquityPoint("c", 90),
                new BacktestResult.EquityPoint("d", 130));

        assertThat(Backtester.maxDrawdown(eq)).isCloseTo(0.25, within(1e-9)); // (120-90)/120
    }

    @Test
    void smaCrossLongInUptrendAndFlatWhileWarmingUp() {
        double[] c = {10, 10, 10, 10, 11, 12, 13, 14, 15, 16};
        boolean[] pos = Backtester.smaCross(c, 2, 4);

        assertThat(pos[0]).isFalse();                 // 데이터 부족 구간
        assertThat(pos[pos.length - 1]).isTrue();     // 상승 추세 → 롱
    }

    @Test
    void rsiIsHundredWhenAllGainsAndZeroWhenAllLosses() {
        double[] up = new double[20];
        double[] down = new double[20];
        for (int i = 0; i < 20; i++) {
            up[i] = 100 + i;
            down[i] = 100 - i;
        }
        assertThat(Backtester.rsiSeries(up, 14)[19]).isCloseTo(100.0, within(1e-6));
        assertThat(Backtester.rsiSeries(down, 14)[19]).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void rsiStrategyCompletesAtLeastOneRoundTripOnVShape() {
        // 급락 후 급반등 → RSI 과매도 진입, 과매수 청산
        int n = 40;
        double[] c = new double[n];
        for (int i = 0; i < 20; i++) {
            c[i] = 100 - 2 * i;          // 100 → 62 하락
        }
        for (int i = 20; i < n; i++) {
            c[i] = c[19] + 3 * (i - 19); // 반등
        }
        boolean[] pos = Backtester.rsi(c, 14, 30, 70);
        BacktestResult r = Backtester.simulate("V", BacktestStrategy.RSI, "", days(n), c, pos, 1000);

        assertThat(r.trades()).isGreaterThanOrEqualTo(1);
        // 과매도 바닥 부근 진입 → 과매수 고점 부근 청산이므로 이익
        assertThat(r.winRate()).isGreaterThan(0.0);
    }
}
