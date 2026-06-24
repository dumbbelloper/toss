package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 성공 응답 공통 envelope. 모든 2xx 응답은 {@code {"result": ...}} 형태로 내려온다.
 *
 * @param <T> result 페이로드 타입
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossEnvelope<T>(T result) {
}
