package com.toss.dashboard;

import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.HoldingsOverview;
import com.toss.client.dto.Order;
import com.toss.service.AccountService;
import com.toss.service.MarketDataService;
import com.toss.service.OrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드용 조회 전용 JSON API.
 * <p>시세/캔들은 토큰만으로 동작하고, 포트폴리오/주문은 {@code toss.account-seq} 가 필요하다
 * (미설정 시 해당 패널만 에러 — 대시보드는 부분 동작).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final AccountService accounts;
    private final OrderQueryService orders;
    private final MarketDataService marketData;

    public DashboardApiController(AccountService accounts, OrderQueryService orders,
                                  MarketDataService marketData) {
        this.accounts = accounts;
        this.orders = orders;
        this.marketData = marketData;
    }

    /** 보유 주식 + 손익 요약. */
    @GetMapping("/portfolio")
    public HoldingsOverview portfolio() {
        return accounts.holdings();
    }

    /** 대기중 주문. */
    @GetMapping("/orders")
    public List<Order> openOrders() {
        return orders.openOrders().orders();
    }

    /** 캔들 차트 데이터. */
    @GetMapping("/candles")
    public CandlePageResponse candles(@RequestParam String symbol,
                                      @RequestParam(defaultValue = "DAY") CandleInterval interval,
                                      @RequestParam(defaultValue = "120") int count) {
        return marketData.candles(symbol, interval, count);
    }
}
