package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /oauth2/token} 성공 응답 (Client Credentials Grant).
 */
public record OAuth2TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
