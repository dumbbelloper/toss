package com.toss.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BearerTokenInterceptorTest {

    private record Fixture(RestClient client, MockRestServiceServer server, TossTokenManager tokenManager) {
    }

    private Fixture newFixture() {
        TossTokenManager tm = mock(TossTokenManager.class);
        RestClient.Builder builder = RestClient.builder().baseUrl("http://t")
                .requestInterceptor(new BearerTokenInterceptor(tm));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(builder.build(), server, tm);
    }

    @Test
    void refreshesTokenAndRetriesOnceOn401() {
        Fixture f = newFixture();
        when(f.tokenManager().accessToken()).thenReturn("old-token", "new-token");
        f.server().expect(requestTo("http://t/x"))
                .andExpect(header("Authorization", "Bearer old-token"))
                .andRespond(withStatus(UNAUTHORIZED));
        f.server().expect(requestTo("http://t/x"))
                .andExpect(header("Authorization", "Bearer new-token")) // 새 토큰으로 재시도
                .andRespond(withSuccess("ok", TEXT_PLAIN));

        String result = f.client().get().uri("/x").retrieve().body(String.class);

        assertThat(result).isEqualTo("ok");
        verify(f.tokenManager()).invalidate();
        verify(f.tokenManager(), times(2)).accessToken();
        f.server().verify();
    }

    @Test
    void noRetryOnSuccess() {
        Fixture f = newFixture();
        when(f.tokenManager().accessToken()).thenReturn("tok");
        f.server().expect(requestTo("http://t/x")).andRespond(withSuccess("ok", TEXT_PLAIN));

        f.client().get().uri("/x").retrieve().body(String.class);

        verify(f.tokenManager(), never()).invalidate();
        f.server().verify();
    }
}
