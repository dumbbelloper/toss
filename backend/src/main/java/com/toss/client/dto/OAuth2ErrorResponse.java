package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /oauth2/token} 실패 응답 (OAuth2 표준 에러 형식).
 * BFF 공통 에러 envelope 와 다르다.
 */
public record OAuth2ErrorResponse(
        String error,
        @JsonProperty("error_description") String errorDescription,
        @JsonProperty("error_uri") String errorUri
) {
}
