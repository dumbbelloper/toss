package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 에러 응답 공통 envelope. 모든 4xx/5xx 응답은 {@code {"error": {...}}} 형태.
 * (단, {@code /oauth2/token} 은 OAuth2 표준 에러 형식을 사용한다.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(ApiError error) {
}
