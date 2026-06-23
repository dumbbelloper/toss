package com.toss.auth;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 모든 API 요청에 {@code Authorization: Bearer {access_token}} 헤더를 주입한다.
 * 토큰 발급 전용 클라이언트에는 적용하지 않는다(순환 방지).
 * <p>401 수신 시 캐시된 토큰을 무효화하고 새 토큰으로 <b>한 번</b> 재시도한다 —
 * 토큰이 외부 요인(클라이언트당 단일 토큰 정책 등)으로 무효화돼도 만료를 기다리지 않고 복구한다.
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
        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            response.close();
            tokenManager.invalidate();
            request.getHeaders().setBearerAuth(tokenManager.accessToken()); // 새 토큰
            response = execution.execute(request, body);
        }
        return response;
    }
}
