package com.toss.monitor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 모니터링 스케줄링 활성화. 실제 폴링은 {@link PriceMonitor} 가 담당하며
 * {@code toss.monitor.enabled=true} 일 때만 빈으로 등록된다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class MonitorConfig {
}
