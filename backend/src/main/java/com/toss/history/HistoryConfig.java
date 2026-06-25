package com.toss.history;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 과거 시세 데이터 소스(Yahoo Finance) 호출용 RestClient.
 * Toss 클라이언트와 분리(인증/레이트리밋 인터셉터 없음). 교체형 — 다른 provider 추가 가능.
 */
@Configuration(proxyBeanMethods = false)
public class HistoryConfig {

    @Bean
    RestClient yahooRestClient() {
        return RestClient.builder()
                .baseUrl("https://query1.finance.yahoo.com")
                // Yahoo 는 기본 Java UA 를 종종 차단 → 브라우저 UA.
                .defaultHeader("User-Agent", "Mozilla/5.0 (gotgan-backtest)")
                .build();
    }
}
