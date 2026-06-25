package com.toss.digest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시장 요약 알림 설정 ({@code toss.digest.*}). 요약 종목은 관심종목(watchlist) 전체를 사용한다.
 *
 * @param enabled 스케줄 발송 활성화 (기본 false)
 * @param cron    발송 cron (KST). 기본 매시 정각
 */
@ConfigurationProperties("toss.digest")
public record DigestProperties(
        boolean enabled,
        String cron
) {
    public DigestProperties {
        if (cron == null || cron.isBlank()) {
            cron = "0 0 * * * *";
        }
    }
}
