package com.toss.backtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 일봉 종가 시계열에 전략을 적용해 매매를 시뮬레이션하고 성과 지표를 계산하는 순수 엔진.
 * <p>모든 신호는 미래를 참조하지 않는다(신호[i] 는 종가[0..i] 만 사용). 포지션은 100% 진입/청산
 * (롱 또는 현금)이며, 신호가 바뀐 봉의 종가로 체결한다고 가정한다.
 */
public final class Backtester {

    private Backtester() {
    }

    /** 전략별 포지션 신호 (true=롱, false=현금). */
    public static boolean[] signals(BacktestStrategy strategy, double[] closes, Params p) {
        return switch (strategy) {
            case BUY_AND_HOLD -> buyAndHold(closes.length);
            case SMA_CROSS -> smaCross(closes, p.shortWindow(), p.longWindow());
            case RSI -> rsi(closes, p.rsiPeriod(), p.rsiBuyBelow(), p.rsiSellAbove());
        };
    }

    static boolean[] buyAndHold(int n) {
        boolean[] pos = new boolean[n];
        Arrays.fill(pos, true);
        return pos;
    }

    static boolean[] smaCross(double[] c, int shortW, int longW) {
        int n = c.length;
        boolean[] pos = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (i >= longW - 1) {
                pos[i] = sma(c, i, shortW) > sma(c, i, longW);
            }
        }
        return pos;
    }

    static double sma(double[] c, int i, int window) {
        double sum = 0;
        for (int k = i - window + 1; k <= i; k++) {
            sum += c[k];
        }
        return sum / window;
    }

    static boolean[] rsi(double[] c, int period, double buyBelow, double sellAbove) {
        double[] rsi = rsiSeries(c, period);
        int n = c.length;
        boolean[] pos = new boolean[n];
        boolean inMarket = false;
        for (int i = 0; i < n; i++) {
            double r = rsi[i];
            if (!Double.isNaN(r)) {
                if (!inMarket && r < buyBelow) {
                    inMarket = true;
                } else if (inMarket && r > sellAbove) {
                    inMarket = false;
                }
            }
            pos[i] = inMarket;
        }
        return pos;
    }

    /** Wilder RSI. period 이전 인덱스는 NaN. */
    static double[] rsiSeries(double[] c, int period) {
        int n = c.length;
        double[] rsi = new double[n];
        Arrays.fill(rsi, Double.NaN);
        if (n <= period) {
            return rsi;
        }
        double gain = 0;
        double loss = 0;
        for (int i = 1; i <= period; i++) {
            double ch = c[i] - c[i - 1];
            if (ch >= 0) {
                gain += ch;
            } else {
                loss -= ch;
            }
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        rsi[period] = rsiFrom(avgGain, avgLoss);
        for (int i = period + 1; i < n; i++) {
            double ch = c[i] - c[i - 1];
            double g = ch > 0 ? ch : 0;
            double l = ch < 0 ? -ch : 0;
            avgGain = (avgGain * (period - 1) + g) / period;
            avgLoss = (avgLoss * (period - 1) + l) / period;
            rsi[i] = rsiFrom(avgGain, avgLoss);
        }
        return rsi;
    }

    private static double rsiFrom(double avgGain, double avgLoss) {
        if (avgLoss == 0) {
            return 100.0;
        }
        return 100.0 - 100.0 / (1.0 + avgGain / avgLoss);
    }

    /** 신호대로 매매를 시뮬레이션해 결과를 만든다. */
    public static BacktestResult simulate(String symbol, BacktestStrategy strategy, String params,
                                          String[] times, double[] closes, boolean[] pos, double capital) {
        int n = closes.length;
        double cash = capital;
        double shares = 0;
        boolean inMarket = false;
        double entryPrice = 0;
        int trades = 0;
        int wins = 0;
        List<BacktestResult.EquityPoint> equity = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            if (pos[i] && !inMarket) {
                shares = cash / closes[i];
                cash = 0;
                inMarket = true;
                entryPrice = closes[i];
            } else if (!pos[i] && inMarket) {
                cash = shares * closes[i];
                shares = 0;
                inMarket = false;
                trades++;
                if (closes[i] > entryPrice) {
                    wins++;
                }
            }
            equity.add(new BacktestResult.EquityPoint(times[i], cash + shares * closes[i]));
        }

        double finalEquity = n == 0 ? capital : equity.getLast().value();
        double totalReturn = finalEquity / capital - 1.0;
        double buyHoldReturn = n == 0 ? 0 : closes[n - 1] / closes[0] - 1.0;
        double mdd = maxDrawdown(equity);
        double winRate = trades > 0 ? (double) wins / trades : 0.0;

        return new BacktestResult(symbol, strategy.name(), params, n, capital, finalEquity,
                totalReturn, buyHoldReturn, mdd, trades, winRate, equity);
    }

    /** 자본 곡선의 최대 낙폭 (peak 대비 최대 하락 비율, 양수). */
    static double maxDrawdown(List<BacktestResult.EquityPoint> equity) {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDd = 0;
        for (BacktestResult.EquityPoint p : equity) {
            peak = Math.max(peak, p.value());
            if (peak > 0) {
                maxDd = Math.max(maxDd, (peak - p.value()) / peak);
            }
        }
        return maxDd;
    }

    /** 전략 파라미터. */
    public record Params(int shortWindow, int longWindow, int rsiPeriod, double rsiBuyBelow, double rsiSellAbove) {
        public static Params defaults() {
            return new Params(5, 20, 14, 30, 70);
        }
    }
}
