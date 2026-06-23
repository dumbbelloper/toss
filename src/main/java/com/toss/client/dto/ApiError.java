package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * 에러 객체. {@code requestId} 는 응답 헤더 {@code X-Request-Id} 와 동일.
 *
 * @param requestId 요청 추적 ID (CS 문의 시 첨부 권장)
 * @param code      에러 코드 (예: {@code stock-not-found}, {@code invalid-token})
 * @param message   에러 메시지
 * @param data      해결 힌트 (코드별로 키 구조 상이, nullable)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiError(
        String requestId,
        String code,
        String message,
        Map<String, Object> data
) {
}
