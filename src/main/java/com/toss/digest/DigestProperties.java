package com.toss.digest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 시장 요약 알림 설정 ({@code toss.digest.*}).
 *
 * @param enabled 스케줄 발송 활성화 (기본 false)
 * @param cron    발송 cron (KST). 기본 매일 08:00
 * @param symbols 요약에 포함할 종목 코드 (지수 프록시·환율은 항상 포함)
 */
@ConfigurationProperties("toss.digest")
public record DigestProperties(
        boolean enabled,
        String cron,
        List<String> symbols
) {
    public DigestProperties {
        symbols = symbols == null ? List.of() : List.copyOf(symbols);
        if (cron == null || cron.isBlank()) {
            cron = "0 0 8 * * *";
        }
    }
}
