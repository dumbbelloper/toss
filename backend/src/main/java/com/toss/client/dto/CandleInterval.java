package com.toss.client.dto;

/** 캔들 간격. API 쿼리 값은 {@link #code()} ("1m"/"1d"). */
public enum CandleInterval {
    MINUTE("1m"),
    DAY("1d");

    private final String code;

    CandleInterval(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
