package com.toss.monitor;

import com.toss.client.dto.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorControllerTest {

    private MonitorController newController(PriceStream stream) {
        var watchlist = new WatchlistService(new MonitorProperties(false, List.of(), 0));
        return new MonitorController(stream, watchlist);
    }

    @Test
    void watchlistAddAndRemove() {
        var controller = newController(new PriceStream());

        assertThat(controller.add("005930")).contains("005930");
        assertThat(controller.watchlist()).contains("005930");
        assertThat(controller.remove("005930")).doesNotContain("005930");
    }

    @Test
    void streamEmitsPublishedTicksAsServerSentEvents() {
        var stream = new PriceStream();
        var controller = newController(stream);

        List<PriceTick> received = new CopyOnWriteArrayList<>();
        controller.stream().subscribe(sse -> received.add(sse.data()));

        var tick = new PriceTick("005930", new BigDecimal("72000"), Currency.KRW, null, Instant.now());
        stream.publish(tick);

        assertThat(received).containsExactly(tick);
    }
}
