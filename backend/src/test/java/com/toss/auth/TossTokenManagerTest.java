package com.toss.auth;

import com.toss.config.TossProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossTokenManagerTest {

    private static final String TOKEN_URI = "http://toss.test/oauth2/token";

    private record Fixture(TossTokenManager manager, MockRestServiceServer server) {
    }

    private Fixture newFixture(Clock clock) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossProperties props = new TossProperties("http://toss.test", "cid", "secret", null);
        TossTokenManager manager = new TossTokenManager(builder.build(), props, clock);
        return new Fixture(manager, server);
    }

    @Test
    void issuesAndReturnsAccessToken() {
        Fixture f = newFixture(Clock.systemUTC());
        f.server().expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-A\",\"token_type\":\"Bearer\",\"expires_in\":3600}",
                        APPLICATION_JSON));

        assertThat(f.manager().accessToken()).isEqualTo("tok-A");
        f.server().verify();
    }

    @Test
    void cachesTokenAcrossCalls() {
        Fixture f = newFixture(Clock.systemUTC());
        // 단 한 번의 발급만 기대 — 두 번째 호출이 캐시를 쓰지 않으면 검증 실패한다.
        f.server().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-A\",\"token_type\":\"Bearer\",\"expires_in\":3600}",
                        APPLICATION_JSON));

        assertThat(f.manager().accessToken()).isEqualTo("tok-A");
        assertThat(f.manager().accessToken()).isEqualTo("tok-A");
        f.server().verify();
    }

    @Test
    void refreshesWhenTokenNearExpiry() {
        // 고정 시계: 첫 토큰은 60초 만료 → skew(30s) 안에 들어와 곧 재발급 대상.
        Instant t0 = Instant.parse("2026-06-23T00:00:00Z");
        MutableClock clock = new MutableClock(t0);
        Fixture f = newFixture(clock);

        f.server().expect(requestTo(TOKEN_URI)).andRespond(withSuccess(
                "{\"access_token\":\"tok-A\",\"token_type\":\"Bearer\",\"expires_in\":60}", APPLICATION_JSON));
        f.server().expect(requestTo(TOKEN_URI)).andRespond(withSuccess(
                "{\"access_token\":\"tok-B\",\"token_type\":\"Bearer\",\"expires_in\":3600}", APPLICATION_JSON));

        assertThat(f.manager().accessToken()).isEqualTo("tok-A");
        clock.advance(Duration.ofSeconds(40)); // 만료 20초 전 → skew 진입
        assertThat(f.manager().accessToken()).isEqualTo("tok-B");
        f.server().verify();
    }

    @Test
    void throwsOnOAuth2Error() {
        Fixture f = newFixture(Clock.systemUTC());
        f.server().expect(requestTo(TOKEN_URI)).andRespond(withStatus(UNAUTHORIZED)
                .contentType(APPLICATION_JSON)
                .body("{\"error\":\"invalid_client\",\"error_description\":\"bad secret\"}"));

        assertThatThrownBy(() -> f.manager().accessToken())
                .isInstanceOf(TossAuthException.class)
                .hasMessageContaining("invalid_client")
                .hasMessageContaining("bad secret");
    }

    @Test
    void throwsWhenCredentialsMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss.test");
        MockRestServiceServer.bindTo(builder).build();
        TossProperties noCreds = new TossProperties("http://toss.test", null, null, null);
        TossTokenManager manager = new TossTokenManager(builder.build(), noCreds, Clock.systemUTC());

        assertThatThrownBy(manager::accessToken)
                .isInstanceOf(TossAuthException.class)
                .hasMessageContaining("client-id");
    }

    /** 테스트용 전진 가능 시계. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
