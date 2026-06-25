package com.toss.backtest;

import com.toss.history.PriceDailyDao;
import com.toss.history.PriceRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BacktestServiceTest {

    private final PriceDailyDao dao = mock(PriceDailyDao.class);
    private final BacktestService service = new BacktestService(dao);

    private static PriceRow row(String date, double close, double adj) {
        return new PriceRow(LocalDate.parse(date), close, adj);
    }

    private BacktestResult run(String symbol, boolean reinvest, String currency) {
        return service.run(symbol, BacktestStrategy.BUY_AND_HOLD, Backtester.Params.defaults(),
                200, 1000, reinvest, currency);
    }

    @Test
    void runsBuyAndHoldOnDbSeries() {
        when(dao.series(eq("SPY"), anyInt())).thenReturn(List.of(
                row("2026-01-01", 100, 100),
                row("2026-01-02", 110, 110),
                row("2026-01-03", 121, 121)));

        BacktestResult r = run("SPY", true, "USD");

        assertThat(r.bars()).isEqualTo(3);
        assertThat(r.buyHoldReturn()).isCloseTo(0.21, within(1e-9)); // 100→121
        assertThat(r.equity()).first().extracting(BacktestResult.EquityPoint::time).isEqualTo("2026-01-01");
    }

    @Test
    void reinvestUsesAdjCloseElseClose() {
        // 가격(close) 100→110 = +10%, 총수익(adj) 100→121 = +21%
        when(dao.series(eq("X"), anyInt())).thenReturn(List.of(
                row("2026-01-01", 100, 100),
                row("2026-01-02", 110, 121)));

        assertThat(run("X", false, "USD").buyHoldReturn()).isCloseTo(0.10, within(1e-9));
        assertThat(run("X", true, "USD").buyHoldReturn()).isCloseTo(0.21, within(1e-9));
    }

    @Test
    void krwAppliesHistoricalFx() {
        when(dao.series(eq("X"), anyInt())).thenReturn(List.of(
                row("2026-01-01", 100, 100),
                row("2026-01-02", 100, 100))); // USD 가격 변화 없음
        TreeMap<LocalDate, Double> fx = new TreeMap<>();
        fx.put(LocalDate.parse("2026-01-01"), 1000.0);
        fx.put(LocalDate.parse("2026-01-02"), 1100.0); // 환율 +10%
        when(dao.fxSeries(eq("USDKRW"), any())).thenReturn(fx);

        assertThat(run("X", true, "USD").buyHoldReturn()).isCloseTo(0.0, within(1e-9));  // 달러 변화 없음
        assertThat(run("X", true, "KRW").buyHoldReturn()).isCloseTo(0.10, within(1e-9)); // 원화 환율 +10%
    }

    @Test
    void rejectsInsufficientData() {
        when(dao.series(any(), anyInt())).thenReturn(List.of(row("2026-01-01", 100, 100)));
        assertThatThrownBy(() -> run("X", true, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부족");
    }
}
