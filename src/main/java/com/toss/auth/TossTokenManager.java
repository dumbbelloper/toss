package com.toss.auth;

import com.toss.client.dto.OAuth2ErrorResponse;
import com.toss.client.dto.OAuth2TokenResponse;
import com.toss.config.TossProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 토스증권 OAuth2 액세스 토큰을 발급·캐싱·자동 갱신한다.
 * <p>Client Credentials Grant 단일 토큰을 메모리에 캐싱하고, 만료 직전
 * ({@link #REFRESH_SKEW}) 에 선제적으로 재발급한다. 스레드 안전.
 */
@Component
public class TossTokenManager {

    private static final Logger log = LoggerFactory.getLogger(TossTokenManager.class);

    /** 만료 이 시간 전부터는 재발급 (시계 오차·네트워크 지연 대비). */
    private static final Duration REFRESH_SKEW = Duration.ofSeconds(30);

    private final RestClient tokenRestClient;
    private final TossProperties props;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile CachedToken cached;

    @Autowired
    public TossTokenManager(RestClient tossTokenRestClient, TossProperties props) {
        this(tossTokenRestClient, props, Clock.systemUTC());
    }

    TossTokenManager(RestClient tokenRestClient, TossProperties props, Clock clock) {
        this.tokenRestClient = tokenRestClient;
        this.props = props;
        this.clock = clock;
    }

    /** 유효한 액세스 토큰을 반환한다(필요 시 동기적으로 재발급). */
    public String accessToken() {
        CachedToken current = cached;
        if (current != null && current.isFresh(clock.instant(), REFRESH_SKEW)) {
            return current.token();
        }
        lock.lock();
        try {
            // 락 획득 사이 다른 스레드가 이미 갱신했는지 재확인 (double-checked)
            current = cached;
            if (current != null && current.isFresh(clock.instant(), REFRESH_SKEW)) {
                return current.token();
            }
            CachedToken refreshed = requestToken();
            cached = refreshed;
            return refreshed.token();
        } finally {
            lock.unlock();
        }
    }

    /** 캐시를 무효화한다(예: 401 수신 후 강제 재발급 유도). */
    public void invalidate() {
        cached = null;
    }

    private CachedToken requestToken() {
        if (isBlank(props.clientId()) || isBlank(props.clientSecret())) {
            throw new TossAuthException(
                    "토스증권 client-id/client-secret 미설정. application-local.yml 또는 환경변수에 설정하세요.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        try {
            OAuth2TokenResponse resp = tokenRestClient.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuth2TokenResponse.class);

            if (resp == null || isBlank(resp.accessToken())) {
                throw new TossAuthException("토큰 응답이 비어 있습니다.");
            }
            Instant expiresAt = clock.instant().plusSeconds(resp.expiresIn());
            log.debug("토스 액세스 토큰 발급 완료 (expires_in={}s)", resp.expiresIn());
            return new CachedToken(resp.accessToken(), expiresAt);
        } catch (RestClientResponseException e) {
            throw new TossAuthException("토큰 발급 실패: " + describe(e), e);
        }
    }

    private static String describe(RestClientResponseException e) {
        try {
            OAuth2ErrorResponse err = e.getResponseBodyAs(OAuth2ErrorResponse.class);
            if (err != null && err.error() != null) {
                return err.error() + (err.errorDescription() != null ? " - " + err.errorDescription() : "");
            }
        } catch (Exception ignored) {
            // 본문 파싱 실패 시 상태 코드로 폴백
        }
        return e.getStatusCode().toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 캐시된 토큰과 만료 시각. */
    private record CachedToken(String token, Instant expiresAt) {
        boolean isFresh(Instant now, Duration skew) {
            return now.isBefore(expiresAt.minus(skew));
        }
    }
}
