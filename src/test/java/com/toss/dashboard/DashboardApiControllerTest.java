package com.toss.dashboard;

import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.Order;
import com.toss.client.dto.PaginatedOrderResponse;
import com.toss.service.AccountService;
import com.toss.service.MarketDataService;
import com.toss.service.OrderQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardApiControllerTest {

    private final AccountService accounts = mock(AccountService.class);
    private final OrderQueryService orders = mock(OrderQueryService.class);
    private final MarketDataService marketData = mock(MarketDataService.class);
    private final DashboardApiController controller = new DashboardApiController(accounts, orders, marketData);

    @Test
    void candlesDelegatesWithDefaults() {
        when(marketData.candles("005930", CandleInterval.DAY, 120))
                .thenReturn(new CandlePageResponse(List.of(), null));

        controller.candles("005930", CandleInterval.DAY, 120);

        verify(marketData).candles("005930", CandleInterval.DAY, 120);
    }

    @Test
    void openOrdersUnwrapsPage() {
        Order o = new Order("o1", "005930", "BUY", "LIMIT", "DAY", "PENDING",
                null, null, null, null, null, null, null);
        when(orders.openOrders()).thenReturn(new PaginatedOrderResponse(List.of(o), null, false));

        assertThat(controller.openOrders()).containsExactly(o);
    }
}
