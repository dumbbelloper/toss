package com.toss.common;

import org.springframework.http.HttpStatusCode;

import java.util.Map;

/**
 * 일시적(재시도 가능) 토스 API 에러 — 429(rate limit) 및 5xx(서버 일시 장애/점검).
 * {@code @Retryable} 의 재시도 대상으로 사용된다. 4xx(검증·상태 충돌 등)는
 * 재시도해도 결과가 같으므로 {@link TossApiException} 으로 남긴다.
 */
public class TossTransientException extends TossApiException {

    public TossTransientException(HttpStatusCode status, String code, String message, String requestId,
                                  Map<String, Object> data) {
        super(status, code, message, requestId, data);
    }
}
