package com.toss.ratelimit;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * API 그룹별 클라이언트측 레이트 리미터. 서버 한도(TPS)를 넘지 않도록 선제적으로 조절한다.
 */
@Component
public class TossRateLimiter {

    private final Map<RateLimitGroup, TokenBucket> buckets = new EnumMap<>(RateLimitGroup.class);

    public TossRateLimiter() {
        for (RateLimitGroup group : RateLimitGroup.values()) {
            buckets.put(group, new TokenBucket(group.permitsPerSecond()));
        }
    }

    /** 해당 그룹의 토큰 1개를 확보할 때까지 블로킹한다. */
    public void acquire(RateLimitGroup group) {
        try {
            buckets.get(group).acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("레이트 리미터 대기 중 인터럽트됨: " + group, e);
        }
    }
}
