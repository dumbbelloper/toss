package com.toss.tax;

/** ETF 과세 3분류. 규칙 단일출처: docs/product/korea-tax.md */
public enum TaxClass {
    /** 국내상장 국내주식형 ETF: 매매차익 비과세, 분배금 15.4%. */
    KR_EQUITY,
    /** 국내상장 해외·채권·원자재 ETF: 매매차익 15.4% 배당(min[실차익,과표기준가증분]), 분배금 15.4%. */
    KR_OTHER,
    /** 해외상장 ETF: 매매차익 22%(연 250만 공제), 분배금 15.4%(현지 원천징수). */
    FOREIGN;

    public static TaxClass of(String s) {
        return switch (s == null ? "" : s.trim().toLowerCase()) {
            case "kr_equity" -> KR_EQUITY;
            case "kr_other" -> KR_OTHER;
            case "foreign" -> FOREIGN;
            default -> throw new IllegalArgumentException("unknown tax_class: " + s);
        };
    }
}
