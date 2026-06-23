package com.toss.auth;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 모든 API 요청에 {@code Authorization: Bearer {access_token}} 헤더를 주입한다.
 * 토큰 발급 전용 클라이언트에는 적용하지 않는다(순환 방지).
 */
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final TossTokenManager tokenManager;

    public BearerTokenInterceptor(TossTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(tokenManager.accessToken());
        return execution.execute(request, body);
    }
}
