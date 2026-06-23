package com.toss.auth;

import com.toss.config.TossProperties;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 계좌·자산·주문 엔드포인트에 {@code X-Tossinvest-Account} 헤더를 주입한다.
 * (계좌 목록 조회 {@code /accounts} 와 시세 API 는 헤더가 불필요.)
 * {@code toss.account-seq} 미설정 시, 헤더가 필요한 경로 호출은 즉시 실패시킨다.
 */
public class AccountHeaderInterceptor implements ClientHttpRequestInterceptor {

    private static final String HEADER = "X-Tossinvest-Account";

    private final TossProperties props;

    public AccountHeaderInterceptor(TossProperties props) {
        this.props = props;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String path = request.getURI().getPath();
        if (requiresAccount(path)) {
            Long seq = props.accountSeq();
            if (seq == null) {
                throw new IllegalStateException(
                        "X-Tossinvest-Account 헤더가 필요하지만 toss.account-seq 가 설정되지 않았습니다. "
                                + "application-local.yml 에 account-seq 를 설정하세요. (경로: " + path + ")");
            }
            request.getHeaders().add(HEADER, String.valueOf(seq));
        }
        return execution.execute(request, body);
    }

    static boolean requiresAccount(String path) {
        return path.equals("/api/v1/holdings")
                || path.startsWith("/api/v1/orders")
                || path.equals("/api/v1/buying-power")
                || path.equals("/api/v1/sellable-quantity")
                || path.equals("/api/v1/commissions");
    }
}
