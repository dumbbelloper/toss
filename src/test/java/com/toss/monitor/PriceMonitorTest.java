package com.toss.monitor;

import com.toss.client.dto.Currency;
import com.toss.client.dto.PriceResponse;
import com.toss.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PriceMonitorTest {

    private final WatchlistService watchlist = mock(WatchlistService.class);
    private final MarketDataService marketData = mock(MarketDataService.class);
    private final PriceStream stream = mock(PriceStream.class);
    private final PriceSnapshotRepository snapshots = mock(PriceSnapshotRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PriceMonitor monitor = new PriceMonitor(watchlist, marketData, stream, snapshots, events);

    @Test
    void publishesPersistsAndEmitsEventPerPrice() {
        when(watchlist.symbols()).thenReturn(Set.of("005930"));
        when(marketData.prices("005930")).thenReturn(List.of(
                new PriceResponse("005930", "2026-03-25T09:30:00+09:00", new BigDecimal("72000"), Currency.KRW)));

        monitor.poll();

        ArgumentCaptor<PriceTick> tick = ArgumentCaptor.forClass(PriceTick.class);
        verify(stream).publish(tick.capture());
        assertThat(tick.getValue().symbol()).isEqualTo("005930");
        assertThat(tick.getValue().lastPrice()).isEqualByComparingTo("72000");
        verify(snapshots).save(any(PriceSnapshot.class));
        verify(events).publishEvent(any(PriceTick.class));
    }

    @Test
    void skipsWhenWatchlistEmpty() {
        when(watchlist.symbols()).thenReturn(Set.of());

        monitor.poll();

        verifyNoInteractions(marketData, stream, snapshots, events);
    }

    @Test
    void swallowsApiErrorsToKeepPolling() {
        when(watchlist.symbols()).thenReturn(Set.of("ZZZ"));
        when(marketData.prices("ZZZ")).thenThrow(new RuntimeException("boom"));

        assertThatCode(monitor::poll).doesNotThrowAnyException();
        verifyNoInteractions(stream, snapshots, events);
    }
}
