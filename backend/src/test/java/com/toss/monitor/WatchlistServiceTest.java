package com.toss.monitor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistServiceTest {

    @Test
    void seedsFromProperties() {
        var ws = new WatchlistService(new MonitorProperties(true, List.of("005930", "AAPL"), 1000));
        assertThat(ws.symbols()).containsExactlyInAnyOrder("005930", "AAPL");
    }

    @Test
    void addAndRemove() {
        var ws = new WatchlistService(new MonitorProperties(false, List.of(), 0));
        assertThat(ws.isEmpty()).isTrue();
        assertThat(ws.add("000660")).isTrue();
        assertThat(ws.add("000660")).isFalse(); // 중복 추가
        assertThat(ws.symbols()).containsExactly("000660");
        assertThat(ws.remove("000660")).isTrue();
        assertThat(ws.remove("000660")).isFalse();
        assertThat(ws.isEmpty()).isTrue();
    }
}
