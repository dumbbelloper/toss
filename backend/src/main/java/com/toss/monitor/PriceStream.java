package com.toss.monitor;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 시세 틱의 인메모리 멀티캐스트 허브. 폴러가 {@link #publish} 하고,
 * SSE 엔드포인트 등 구독자가 {@link #flux} 로 수신한다 (구독 시점 이후 틱만 전달).
 */
@Component
public class PriceStream {

    // directBestEffort: 라이브 스트림. 구독자가 없으면 드롭하고, 늦은 구독자에게 과거 틱을 재전송하지 않는다.
    private final Sinks.Many<PriceTick> sink = Sinks.many().multicast().directBestEffort();

    /** 틱 발행. 구독자가 없으면 조용히 무시된다. */
    public void publish(PriceTick tick) {
        sink.tryEmitNext(tick);
    }

    public Flux<PriceTick> flux() {
        return sink.asFlux();
    }
}
