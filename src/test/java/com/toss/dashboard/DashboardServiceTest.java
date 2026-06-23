package com.toss.dashboard;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.Currency;
import com.toss.client.dto.HoldingsItem;
import com.toss.client.dto.HoldingsOverview;
import com.toss.client.dto.MarketCountry;
import com.toss.client.dto.Order;
import com.toss.client.dto.OrderExecution;
import com.toss.client.dto.PaginatedOrderResponse;
import com.toss.service.AccountService;
import com.toss.service.MarketDataService;
import com.toss.service.OrderQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final AccountService accounts = mock(AccountService.class);
    private final OrderQueryService orders = mock(OrderQueryService.class);
    private final MarketDataService marketData = mock(MarketDataService.class);
    private final DashboardService service = new DashboardService(accounts, orders, marketData);

    private HoldingsItem holding(String symbol, String name, String avgPrice) {
        return new HoldingsItem(symbol, name, MarketCountry.KR, Currency.KRW,
                new BigDecimal("10"), new BigDecimal("72800"),
                avgPrice == null ? null : new BigDecimal(avgPrice),
                null, null, null, null);
    }

    private Candle candle(String date, String close) {
        return new Candle(date + "T00:00:00+09:00", new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal("1000"), Currency.KRW);
    }

    @Test
    void normalizesToCostBasisAndDerivesPurchaseDate() {
        when(accounts.holdings()).thenReturn(new HoldingsOverview(null, null, null, null,
                List.of(holding("005930", "삼성전자", "70000"))));
        when(marketData.candles(eq("005930"), eq(CandleInterval.DAY), any(Integer.class)))
                .thenReturn(new CandlePageResponse(List.of(
                        candle("2026-03-24", "70000"),
                        candle("2026-03-25", "72800")), null));
        Order buy = new Order("o1", "005930", "BUY", "LIMIT", "DAY", "FILLED", null,
                new BigDecimal("10"), null, Currency.KRW, "2026-03-22T09:00:00+09:00", null,
                new OrderExecution(new BigDecimal("10"), new BigDecimal("70000"), null, null, null,
                        "2026-03-22T09:30:05+09:00", "2026-03-24"));
        when(orders.closedOrders(null, 100)).thenReturn(new PaginatedOrderResponse(List.of(buy), null, false));

        List<ComparisonSeries> result = service.comparison(120);

        assertThat(result).singleElement().satisfies(s -> {
            assertThat(s.symbol()).isEqualTo("005930");
            assertThat(s.purchaseDate()).isEqualTo("2026-03-22");
            assertThat(s.points()).hasSize(2);
            assertThat(s.points().getFirst().value()).isCloseTo(100.0, within(0.001));   // 70000/70000
            assertThat(s.points().getLast().value()).isCloseTo(104.0, within(0.001));    // 72800/70000
        });
    }

    @Test
    void skipsHoldingsWithoutCostBasis() {
        when(accounts.holdings()).thenReturn(new HoldingsOverview(null, null, null, null,
                List.of(holding("005930", "삼성", null))));
        when(orders.closedOrders(null, 100)).thenReturn(new PaginatedOrderResponse(List.of(), null, false));

        assertThat(service.comparison(120)).isEmpty();
    }

    @Test
    void purchaseDateOptionalWhenOrderHistoryUnavailable() {
        when(accounts.holdings()).thenReturn(new HoldingsOverview(null, null, null, null,
                List.of(holding("005930", "삼성", "70000"))));
        when(marketData.candles(any(), any(), any()))
                .thenReturn(new CandlePageResponse(List.of(candle("2026-03-25", "70000")), null));
        when(orders.closedOrders(null, 100)).thenThrow(new IllegalStateException("account-seq 미설정"));

        List<ComparisonSeries> result = service.comparison(120);

        assertThat(result).singleElement().satisfies(s -> assertThat(s.purchaseDate()).isNull());
    }
}
