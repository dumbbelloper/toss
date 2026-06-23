package com.toss.digest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 설정된 cron(KST)에 시장 요약을 발송한다. {@code toss.digest.enabled=true} 일 때만 등록.
 */
@Component
@ConditionalOnProperty(name = "toss.digest.enabled", havingValue = "true")
public class DigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(DigestScheduler.class);

    private final MarketDigestService digest;

    public DigestScheduler(MarketDigestService digest) {
        this.digest = digest;
    }

    @Scheduled(cron = "${toss.digest.cron:0 0 8 * * *}", zone = "Asia/Seoul")
    public void send() {
        try {
            digest.sendDigest();
            log.info("시장 요약 발송 완료");
        } catch (Exception e) {
            log.warn("시장 요약 발송 실패: {}", e.toString());
        }
    }
}
