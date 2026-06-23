package com.toss.common;

import com.toss.client.dto.ApiError;
import com.toss.client.dto.ErrorResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * 토스증권 API 가 반환한 에러 envelope({@code {"error": {...}}}) 를 표현하는 예외.
 * 호출 측은 {@link #code()} 로 에러를 분기 처리한다(예: {@code order-not-found}).
 */
public class TossApiException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;
    private final String requestId;
    private final transient Map<String, Object> data;

    public TossApiException(HttpStatusCode status, String code, String message, String requestId,
                            Map<String, Object> data) {
        super("[" + status + "] " + code + (message != null ? ": " + message : ""));
        this.status = status;
        this.code = code;
        this.requestId = requestId;
        this.data = data;
    }

    /**
     * RestClient 가 던진 4xx/5xx 예외를 토스 에러 envelope 로 해석해 변환한다.
     * 429/5xx 는 재시도 가능한 {@link TossTransientException} 으로 분류한다.
     */
    public static TossApiException from(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        String code = "unknown";
        String message = e.getStatusText();
        String requestId = null;
        Map<String, Object> data = null;
        try {
            ErrorResponse body = e.getResponseBodyAs(ErrorResponse.class);
            if (body != null && body.error() != null) {
                ApiError err = body.error();
                code = err.code();
                message = err.message();
                requestId = err.requestId();
                data = err.data();
            }
        } catch (Exception ignored) {
            // envelope 파싱 불가 → 상태 코드 기반 폴백
        }
        return isTransient(status)
                ? new TossTransientException(status, code, message, requestId, data)
                : new TossApiException(status, code, message, requestId, data);
    }

    /** 429(rate limit) 또는 5xx(서버 일시 장애)는 재시도 대상. */
    private static boolean isTransient(HttpStatusCode status) {
        return status.value() == 429 || status.is5xxServerError();
    }

    public HttpStatusCode status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String requestId() {
        return requestId;
    }

    public Map<String, Object> data() {
        return data;
    }
}
