package com.toss.ratelimit;

/**
 * 단순 토큰 버킷. 초당 {@code refillPerSec} 개씩 충전되며 버스트 용량은 동일 값이다.
 * {@link #acquire()} 는 토큰이 생길 때까지 블로킹한다(가상 스레드 환경 가정).
 * 스레드 안전.
 */
final class TokenBucket {

    private final double maxTokens;
    private final double refillPerSec;
    private double tokens;
    private long lastNanos;

    TokenBucket(double permitsPerSecond) {
        this.maxTokens = permitsPerSecond;
        this.refillPerSec = permitsPerSecond;
        this.tokens = permitsPerSecond; // 시작 시 버스트 가득
        this.lastNanos = System.nanoTime();
    }

    /** 토큰 1개를 소비한다. 없으면 충전될 때까지 대기. */
    synchronized void acquire() throws InterruptedException {
        while (true) {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return;
            }
            double deficit = 1.0 - tokens;
            long waitMs = (long) Math.ceil(deficit / refillPerSec * 1000.0);
            wait(Math.max(1L, waitMs)); // 모니터 해제 후 대기, 타임아웃 시 재확인
        }
    }

    private void refill() {
        long now = System.nanoTime();
        if (now > lastNanos) {
            double added = (now - lastNanos) / 1_000_000_000.0 * refillPerSec;
            tokens = Math.min(maxTokens, tokens + added);
            lastNanos = now;
        }
    }
}
