package com.toss.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 시세 모니터링 설정 ({@code toss.monitor.*}).
 *
 * @param enabled        폴링 활성화 여부 (기본 false)
 * @param symbols        초기 관심종목 (watchlist 시드)
 * @param pollIntervalMs 폴링 주기 ms (기본 2000)
 */
@ConfigurationProperties("toss.monitor")
public record MonitorProperties(
        boolean enabled,
        List<String> symbols,
        long pollIntervalMs
) {
    public MonitorProperties {
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 2000;
        }
    }
}
