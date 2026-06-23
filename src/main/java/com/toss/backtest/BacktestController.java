package com.toss.backtest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 백테스트 API. 토큰만으로 동작(시세 캔들 기반, 계좌 불필요).
 * 예: {@code GET /api/backtest?symbol=005930&strategy=SMA_CROSS&shortWindow=5&longWindow=20}
 */
@RestController
@RequestMapping("/api/backtest")
public class BacktestController {

    private final BacktestService service;

    public BacktestController(BacktestService service) {
        this.service = service;
    }

    @GetMapping
    public BacktestResult run(@RequestParam String symbol,
                              @RequestParam(defaultValue = "BUY_AND_HOLD") BacktestStrategy strategy,
                              @RequestParam(defaultValue = "5") int shortWindow,
                              @RequestParam(defaultValue = "20") int longWindow,
                              @RequestParam(defaultValue = "14") int rsiPeriod,
                              @RequestParam(defaultValue = "30") double rsiBuyBelow,
                              @RequestParam(defaultValue = "70") double rsiSellAbove,
                              @RequestParam(defaultValue = "200") int count,
                              @RequestParam(defaultValue = "1000000") double capital) {
        Backtester.Params params = new Backtester.Params(shortWindow, longWindow, rsiPeriod, rsiBuyBelow, rsiSellAbove);
        return service.run(symbol, strategy, params, count, capital);
    }
}
