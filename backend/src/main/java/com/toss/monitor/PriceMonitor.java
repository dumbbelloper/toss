package com.toss.monitor;

import com.toss.client.dto.PriceResponse;
import com.toss.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * 관심종목 현재가를 주기적으로 폴링해 {@link PriceStream} 으로 발행하고, 스냅샷을 저장하며,
 * {@link PriceTick} 애플리케이션 이벤트를 게시한다(전략·알림 구독용).
 * <p>{@code toss.monitor.enabled=true} 일 때만 활성화된다. 폴링 실패는 삼켜서 다음 주기로 이어간다.
 */
@Component
@ConditionalOnProperty(name = "toss.monitor.enabled", havingValue = "true")
public class PriceMonitor {

    private static final Logger log = LoggerFactory.getLogger(PriceMonitor.class);

    private final WatchlistService watchlist;
    private final MarketDataService marketData;
    private final PriceStream stream;
    private final PriceSnapshotRepository snapshots;
    private final ApplicationEventPublisher events;

    public PriceMonitor(WatchlistService watchlist, MarketDataService marketData, PriceStream stream,
                        PriceSnapshotRepository snapshots, ApplicationEventPublisher events) {
        this.watchlist = watchlist;
        this.marketData = marketData;
        this.stream = stream;
        this.snapshots = snapshots;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${toss.monitor.poll-interval-ms:2000}")
    public void poll() {
        Set<String> symbols = watchlist.symbols();
        if (symbols.isEmpty()) {
            return;
        }
        List<PriceResponse> prices;
        try {
            prices = marketData.prices(symbols.toArray(String[]::new));
        } catch (Exception e) {
            log.warn("시세 폴링 실패 (다음 주기 재시도): {}", e.toString());
            return;
        }
        Instant now = Instant.now();
        for (PriceResponse p : prices) {
            PriceTick tick = new PriceTick(p.symbol(), p.lastPrice(), p.currency(), p.timestamp(), now);
            stream.publish(tick);
            snapshots.save(PriceSnapshot.of(p.symbol(), p.lastPrice(), p.currency(), parseInstant(p.timestamp()), now));
            events.publishEvent(tick);
        }
    }

    private static Instant parseInstant(String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
