package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 주문 목록 페이징 응답. {@code status=OPEN} 은 모든 대기중 주문을 반환(페이징 없음),
 * {@code status=CLOSED} 는 cursor 로 페이징한다.
 *
 * @param orders     주문 목록
 * @param nextCursor 다음 페이지 커서 (없으면 null)
 * @param hasNext    다음 페이지 존재 여부
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginatedOrderResponse(
        List<Order> orders,
        String nextCursor,
        boolean hasNext
) {
}
