package com.toss.monitor;

import com.toss.client.dto.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PriceStreamTest {

    private static PriceTick tick(String symbol, String price) {
        return new PriceTick(symbol, new BigDecimal(price), Currency.KRW, null, Instant.now());
    }

    @Test
    void deliversTicksToActiveSubscriber() {
        var stream = new PriceStream();
        List<PriceTick> received = new CopyOnWriteArrayList<>();
        stream.flux().subscribe(received::add);

        var t = tick("005930", "72000");
        stream.publish(t);

        assertThat(received).containsExactly(t);
    }

    @Test
    void lateSubscriberOnlyGetsSubsequentTicks() {
        var stream = new PriceStream();
        stream.publish(tick("005930", "100")); // 구독자 없음 → 유실

        List<PriceTick> received = new CopyOnWriteArrayList<>();
        stream.flux().subscribe(received::add);
        var t2 = tick("005930", "200");
        stream.publish(t2);

        assertThat(received).containsExactly(t2);
    }
}
