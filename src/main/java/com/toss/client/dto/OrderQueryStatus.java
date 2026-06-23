package com.toss.client.dto;

/** 주문 목록 조회 필터. OPEN: 대기중, CLOSED: 종료(체결/취소/거부 등). */
public enum OrderQueryStatus {
    OPEN,
    CLOSED
}
