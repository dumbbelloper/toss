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

    /** RestClient 가 던진 4xx/5xx 예외를 토스 에러 envelope 로 해석해 변환한다. */
    public static TossApiException from(RestClientResponseException e) {
        try {
            ErrorResponse body = e.getResponseBodyAs(ErrorResponse.class);
            if (body != null && body.error() != null) {
                ApiError err = body.error();
                return new TossApiException(e.getStatusCode(), err.code(), err.message(),
                        err.requestId(), err.data());
            }
        } catch (Exception ignored) {
            // envelope 파싱 불가 → 상태 코드 기반 폴백
        }
        return new TossApiException(e.getStatusCode(), "unknown", e.getStatusText(), null, null);
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
