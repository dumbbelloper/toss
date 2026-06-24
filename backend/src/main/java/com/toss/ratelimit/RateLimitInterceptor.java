package com.toss.ratelimit;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 요청 경로로부터 Rate Limit 그룹을 판별해, 토큰 확보 전까지 요청을 보류한다.
 */
public class RateLimitInterceptor implements ClientHttpRequestInterceptor {

    private final TossRateLimiter rateLimiter;

    public RateLimitInterceptor(TossRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        RateLimitGroup group = RateLimitGroup.resolve(request.getMethod(), request.getURI().getPath());
        rateLimiter.acquire(group);
        return execution.execute(request, body);
    }
}
