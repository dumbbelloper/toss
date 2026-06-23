package com.toss.monitor;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Set;

/**
 * 모니터링 API. 실시간 시세 SSE 스트림과 관심종목 관리.
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final PriceStream stream;
    private final WatchlistService watchlist;

    public MonitorController(PriceStream stream, WatchlistService watchlist) {
        this.stream = stream;
        this.watchlist = watchlist;
    }

    /** 실시간 시세 SSE 스트림 (event: price). */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceTick>> stream() {
        return stream.flux()
                .map(tick -> ServerSentEvent.builder(tick).event("price").build());
    }

    /** 현재 관심종목. */
    @GetMapping("/watchlist")
    public Set<String> watchlist() {
        return watchlist.symbols();
    }

    /** 관심종목 추가. 갱신된 목록 반환. */
    @PutMapping("/watchlist/{symbol}")
    public Set<String> add(@PathVariable String symbol) {
        watchlist.add(symbol);
        return watchlist.symbols();
    }

    /** 관심종목 삭제. 갱신된 목록 반환. */
    @DeleteMapping("/watchlist/{symbol}")
    public Set<String> remove(@PathVariable String symbol) {
        watchlist.remove(symbol);
        return watchlist.symbols();
    }
}
