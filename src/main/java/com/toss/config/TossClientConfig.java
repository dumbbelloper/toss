package com.toss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 토스증권 API 호출용 {@link RestClient} 구성.
 * <p>현재는 토큰 발급 전용 클라이언트만 제공한다. 인증/레이트리밋 인터셉터가 적용된
 * API 호출용 클라이언트는 후속 단계에서 추가된다.
 */
@Configuration(proxyBeanMethods = false)
public class TossClientConfig {

    /**
     * {@code /oauth2/token} 전용 클라이언트.
     * 인증 인터셉터를 적용하지 않는다(토큰 발급 자체는 Bearer 토큰이 불필요하며,
     * 적용 시 순환 의존이 발생).
     */
    @Bean
    RestClient tossTokenRestClient(TossProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}
