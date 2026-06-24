package com.toss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스증권 Open API 연동 설정.
 * <ul>
 *   <li>{@code base-url} 은 application.yml 에 정의.</li>
 *   <li>{@code client-id} / {@code client-secret} / {@code account-seq} 는
 *       gitignore 된 application-local.yml 에 정의 (운영은 환경변수).</li>
 * </ul>
 * 자격증명은 실제 API 호출 시점에 검증한다(없어도 컨텍스트는 기동되도록).
 */
@ConfigurationProperties(prefix = "toss")
public record TossProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        Long accountSeq
) {
}
