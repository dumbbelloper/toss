package com.toss.smoke;

import com.toss.client.dto.PriceResponse;
import com.toss.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기동 시 현재가를 1회 조회해 전체 파이프라인(토큰 발급 → 레이트리밋 → 호출 → 파싱)을 검증한다.
 * 기본 비활성. {@code toss.smoke.enabled=true} 일 때만 동작하며, 실패해도 애플리케이션 기동은
 * 막지 않는다(로그만 남김).
 */
@Component
@ConditionalOnProperty(name = "toss.smoke.enabled", havingValue = "true")
public class TossSmokeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TossSmokeRunner.class);

    private final MarketDataService marketData;
    private final String symbol;

    public TossSmokeRunner(MarketDataService marketData,
                           @org.springframework.beans.factory.annotation.Value("${toss.smoke.symbol:005930}")
                           String symbol) {
        this.marketData = marketData;
        this.symbol = symbol;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("[smoke] 현재가 조회 시작: symbol={}", symbol);
            List<PriceResponse> prices = marketData.prices(symbol);
            prices.forEach(p -> log.info("[smoke] {} = {} {} (ts={})",
                    p.symbol(), p.lastPrice(), p.currency(), p.timestamp()));
            log.info("[smoke] 성공 — 토스 API 연동 정상");
        } catch (Exception e) {
            log.warn("[smoke] 실패 — 자격증명/시장 운영시간/네트워크 확인 필요: {}", e.toString());
        }
    }
}
