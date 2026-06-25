package com.toss.dashboard;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.HoldingsItem;
import com.toss.client.dto.Order;
import com.toss.service.AccountService;
import com.toss.service.MarketDataService;
import com.toss.service.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 분석 — 보유 종목의 매수가 대비 성과 비교 시계열을 만든다.
 * 각 종목의 일봉 종가를 매수평균가로 정규화(100=손익분기)해 한 차트에서 비교할 수 있게 한다.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final AccountService accounts;
    private final OrderQueryService orders;
    private final MarketDataService marketData;

    public DashboardService(AccountService accounts, OrderQueryService orders, MarketDataService marketData) {
        this.accounts = accounts;
        this.orders = orders;
        this.marketData = marketData;
    }

    /** 보유 종목별 "매수가 대비 성과" 시계열 (최근 {@code count} 일봉). */
    public List<ComparisonSeries> comparison(int count) {
        List<HoldingsItem> items = accounts.holdings().items();
        Map<String, String> purchaseDates = bestEffortPurchaseDates();

        List<ComparisonSeries> result = new ArrayList<>();
        for (HoldingsItem item : items) {
            BigDecimal base = item.averagePurchasePrice();
            if (base == null || base.signum() <= 0) {
                continue;
            }
            List<Candle> candles = marketData.candles(item.symbol(), CandleInterval.DAY, count).candles();
            List<ComparisonSeries.Point> points = candles.stream()
                    .filter(c -> c.closePrice() != null && c.timestamp() != null)
                    .map(c -> new ComparisonSeries.Point(
                            c.timestamp().substring(0, 10),
                            c.closePrice().divide(base, 6, RoundingMode.HALF_UP).doubleValue() * 100.0))
                    .sorted(Comparator.comparing(ComparisonSeries.Point::time))
                    .toList();
            result.add(new ComparisonSeries(item.symbol(), item.name(), base,
                    purchaseDates.get(item.symbol()), points));
        }
        return result;
    }

    /** 주문 이력에서 종목별 최초 매수 체결일을 best-effort 로 도출 (account-seq 미설정/이력 없음 시 빈 맵). */
    private Map<String, String> bestEffortPurchaseDates() {
        try {
            List<Order> closed = orders.closedOrders(null, 100).orders();
            Map<String, String> earliest = new HashMap<>();
            for (Order o : closed) {
                if (!"BUY".equals(o.side()) || o.execution() == null || o.execution().filledAt() == null) {
                    continue;
                }
                String date = o.execution().filledAt().substring(0, 10);
                earliest.merge(o.symbol(), date, (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
            return earliest;
        } catch (Exception e) {
            log.debug("매수일자 도출 생략 (이력 조회 불가): {}", e.toString());
            return Map.of();
        }
    }
}
