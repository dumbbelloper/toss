package com.toss.config;

import com.toss.auth.AccountHeaderInterceptor;
import com.toss.auth.BearerTokenInterceptor;
import com.toss.auth.TossTokenManager;
import com.toss.client.AccountClient;
import com.toss.client.MarketDataClient;
import com.toss.client.OrderInfoClient;
import com.toss.ratelimit.RateLimitInterceptor;
import com.toss.ratelimit.TossRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * 토스증권 API 호출용 {@link RestClient} 및 선언형 클라이언트 구성.
 */
@Configuration(proxyBeanMethods = false)
public class TossClientConfig {

    /**
     * {@code /oauth2/token} 전용 클라이언트.
     * 인증/레이트리밋 인터셉터를 적용하지 않는다(토큰 발급에는 Bearer 가 불필요하며,
     * 적용 시 순환 의존이 발생).
     */
    @Bean
    RestClient tossTokenRestClient(TossProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }

    /**
     * 인증된 API 호출용 클라이언트.
     * 인터셉터 순서: 레이트리밋(토큰 확보) → 계좌 헤더 주입 → Bearer 주입 → 실행.
     */
    @Bean
    RestClient tossApiRestClient(TossProperties props, TossTokenManager tokenManager,
                                 TossRateLimiter rateLimiter) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestInterceptor(new RateLimitInterceptor(rateLimiter))
                .requestInterceptor(new AccountHeaderInterceptor(props))
                .requestInterceptor(new BearerTokenInterceptor(tokenManager))
                .build();
    }

    @Bean
    MarketDataClient marketDataClient(RestClient tossApiRestClient) {
        return createClient(tossApiRestClient, MarketDataClient.class);
    }

    @Bean
    AccountClient accountClient(RestClient tossApiRestClient) {
        return createClient(tossApiRestClient, AccountClient.class);
    }

    @Bean
    OrderInfoClient orderInfoClient(RestClient tossApiRestClient) {
        return createClient(tossApiRestClient, OrderInfoClient.class);
    }

    private static <T> T createClient(RestClient restClient, Class<T> type) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }
}
