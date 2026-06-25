package com.toss.sim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 은퇴 현금흐름 시뮬레이터 API (retiremoni 이식). 시장 데이터 불필요(규칙 기반 계산).
 */
@RestController
@RequestMapping("/api/sim")
public class SimController {

    private final DividendSimService dividend;

    public SimController(DividendSimService dividend) {
        this.dividend = dividend;
    }

    /** SIM01: ETF 세후 월배당. 예: {@code /api/sim/dividend?principal=100000000&yieldPercent=8} */
    @GetMapping("/dividend")
    public DividendResult dividend(@RequestParam long principal,
                                   @RequestParam(defaultValue = "8") double yieldPercent,
                                   @RequestParam(defaultValue = "0") long otherFinancialIncome) {
        return dividend.dividend(principal, yieldPercent, otherFinancialIncome);
    }
}
