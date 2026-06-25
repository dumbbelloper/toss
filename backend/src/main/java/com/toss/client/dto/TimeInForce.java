package com.toss.client.dto;

/**
 * 주문 유효 조건 (Time In Force). orderType 와 결합되어 주문 방식 결정.
 * - DAY: 당일 유효
 * - CLS: 장 마감 주문 (현재 US + LIMIT 조합만 지원, = LOC)
 * - OPG: 장 시작 주문
 */
public enum TimeInForce {
    DAY,
    CLS,
    OPG
}
