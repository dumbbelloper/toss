package com.toss.backtest;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.Currency;
import com.toss.service.MarketDataService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BacktestServiceTest {

    private final MarketDataService marketData = mock(MarketDataService.class);
    private final BacktestService service = new BacktestService(marketData);

    private static Candle candle(String date, String close) {
        return new Candle(date + "T00:00:00+09:00", new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal("1"), Currency.KRW);
    }

    @Test
    void sortsCandlesAscendingAndRunsStrategy() {
        // 응답이 최신순(내림차순)이어도 과거→현재로 정렬해야 한다
        when(marketData.candles(eq("005930"), eq(CandleInterval.DAY), any()))
                .thenReturn(new CandlePageResponse(List.of(
                        candle("2026-01-03", "121"),
                        candle("2026-01-02", "110"),
                        candle("2026-01-01", "100")), null));

        BacktestResult r = service.run("005930", BacktestStrategy.BUY_AND_HOLD,
                Backtester.Params.defaults(), 200, 1000);

        assertThat(r.bars()).isEqualTo(3);
        assertThat(r.buyHoldReturn()).isCloseTo(0.21, within(1e-9)); // 100→121
        assertThat(r.equity()).first().extracting(BacktestResult.EquityPoint::time).isEqualTo("2026-01-01");
    }

    @Test
    void rejectsInsufficientCandles() {
        when(marketData.candles(any(), any(), any()))
                .thenReturn(new CandlePageResponse(List.of(candle("2026-01-01", "100")), null));

        assertThatThrownBy(() -> service.run("X", BacktestStrategy.BUY_AND_HOLD,
                Backtester.Params.defaults(), 200, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("부족");
    }
}
