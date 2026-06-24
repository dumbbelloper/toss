package com.toss.backtest;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandleInterval;
import com.toss.service.MarketDataService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 토스 일봉 캔들로 전략 백테스트를 수행한다.
 */
@Service
public class BacktestService {

    private final MarketDataService marketData;

    public BacktestService(MarketDataService marketData) {
        this.marketData = marketData;
    }

    public BacktestResult run(String symbol, BacktestStrategy strategy, Backtester.Params params,
                              int count, double capital) {
        List<Candle> sorted = marketData.candles(symbol, CandleInterval.DAY, count).candles().stream()
                .filter(c -> c.closePrice() != null && c.timestamp() != null)
                .sorted(Comparator.comparing(Candle::timestamp)) // 과거→현재
                .toList();
        int n = sorted.size();
        if (n < 2) {
            throw new IllegalArgumentException("백테스트할 캔들이 부족합니다 (" + n + "개). count 를 늘리거나 종목을 확인하세요.");
        }

        String[] times = new String[n];
        double[] closes = new double[n];
        for (int i = 0; i < n; i++) {
            times[i] = sorted.get(i).timestamp().substring(0, 10);
            closes[i] = sorted.get(i).closePrice().doubleValue();
        }

        boolean[] pos = Backtester.signals(strategy, closes, params);
        return Backtester.simulate(symbol, strategy, describe(strategy, params), times, closes, pos, capital);
    }

    private static String describe(BacktestStrategy strategy, Backtester.Params p) {
        return switch (strategy) {
            case BUY_AND_HOLD -> "매수 후 보유";
            case SMA_CROSS -> "SMA " + p.shortWindow() + "/" + p.longWindow();
            case RSI -> "RSI " + p.rsiPeriod() + " (<" + (int) p.rsiBuyBelow() + " 매수, >" + (int) p.rsiSellAbove() + " 매도)";
        };
    }
}
