package com.toss.backtest;

import com.toss.history.PriceDailyDao;
import com.toss.history.PriceRow;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;

/**
 * 내부 DB(price_daily)의 일별 시세로 전략 백테스트를 수행한다(라이브 호출 X).
 *
 * <p>두 축의 옵션:
 * <ul>
 *   <li><b>배당 재투자</b> — {@code reinvest=true} 면 adj_close(총수익), false 면 close(가격수익).
 *   <li><b>통화</b> — {@code currency=KRW} 면 각 날짜의 USDKRW 환율을 곱해 원화 기준, USD 면 원본.
 * </ul>
 * 즉 "달러로 얼마 벌었나" vs "그 당시 환율 적용 시 원화로 얼마 벌었나" 를 모두 본다.
 */
@Service
public class BacktestService {

    private final PriceDailyDao dao;

    public BacktestService(PriceDailyDao dao) {
        this.dao = dao;
    }

    public BacktestResult run(String symbol, BacktestStrategy strategy, Backtester.Params params,
                              int count, double capital, boolean reinvest, String currency) {
        List<PriceRow> rows = dao.series(symbol, count);
        int n = rows.size();
        if (n < 2) {
            throw new IllegalArgumentException(
                    "DB 에 '" + symbol + "' 시세가 부족합니다 (" + n + "개). 유니버스에 추가 후 백필이 필요합니다.");
        }

        boolean krw = "KRW".equalsIgnoreCase(currency);
        NavigableMap<LocalDate, Double> fx = krw
                ? dao.fxSeries("USDKRW", rows.getLast().date())
                : null;

        String[] times = new String[n];
        double[] prices = new double[n];
        for (int i = 0; i < n; i++) {
            PriceRow r = rows.get(i);
            double price = reinvest ? r.adjClose() : r.close();
            if (krw && fx != null) {
                Double rate = floor(fx, r.date());
                if (rate != null) {
                    price *= rate;
                }
            }
            times[i] = r.date().toString();
            prices[i] = price;
        }

        boolean[] pos = Backtester.signals(strategy, prices, params);
        return Backtester.simulate(symbol, strategy, describe(strategy, params, reinvest, krw),
                times, prices, pos, capital);
    }

    /** 해당 날짜 이하의 가장 최근 환율(주말·휴일 forward-fill). 초기 결측이면 가장 이른 값. */
    private static Double floor(NavigableMap<LocalDate, Double> fx, LocalDate date) {
        var e = fx.floorEntry(date);
        if (e == null) {
            e = fx.ceilingEntry(date);
        }
        return e == null ? null : e.getValue();
    }

    private static String describe(BacktestStrategy strategy, Backtester.Params p,
                                   boolean reinvest, boolean krw) {
        String s = switch (strategy) {
            case BUY_AND_HOLD -> "매수 후 보유";
            case SMA_CROSS -> "SMA " + p.shortWindow() + "/" + p.longWindow();
            case RSI -> "RSI " + p.rsiPeriod() + " (<" + (int) p.rsiBuyBelow() + " 매수, >" + (int) p.rsiSellAbove() + " 매도)";
        };
        return s + " · " + (reinvest ? "배당재투자" : "가격") + " · " + (krw ? "KRW" : "USD");
    }
}
