package com.toss.backtest;

/** 백테스트 전략. */
public enum BacktestStrategy {
    /** 매수 후 보유 (벤치마크). */
    BUY_AND_HOLD,
    /** 단기/장기 이동평균 골든·데드크로스. */
    SMA_CROSS,
    /** RSI 과매도 매수 · 과매수 매도. */
    RSI
}
