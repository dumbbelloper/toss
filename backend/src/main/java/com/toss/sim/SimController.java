package com.toss.sim;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 시뮬레이터 API. SIM01 = 배당(실분배금 세후 현금흐름). 비교 모드는 클라이언트가 심볼 2개로 2회 호출. */
@RestController
@RequestMapping("/api/sim")
public class SimController {

    private final DividendSimService sim;
    private final AccountSimService account;

    public SimController(DividendSimService sim, AccountSimService account) {
        this.sim = sim;
        this.account = account;
    }

    @GetMapping("/dividend")
    public DividendSimService.Result dividend(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "LUMP_SUM") DividendSimService.Contribution contribution,
            @RequestParam(defaultValue = "10000000") double amount,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "true") boolean reinvest) {
        return sim.run(new DividendSimService.Params(
                symbol, contribution, amount, LocalDate.parse(start), LocalDate.parse(end), reinvest));
    }

    /** SIM02: 계좌유형(일반·ISA·연금) N년 세후 비교. */
    @GetMapping("/account-compare")
    public AccountSimService.CompareResult accountCompare(
            @RequestParam(defaultValue = "0") double lumpSum,
            @RequestParam(defaultValue = "9000000") double annualContribution,
            @RequestParam(defaultValue = "30") int years,
            @RequestParam(defaultValue = "0.06") double annualReturn,
            @RequestParam(defaultValue = "0.02") double dividendYield,
            @RequestParam(defaultValue = "false") boolean lowIncome,
            @RequestParam(defaultValue = "65") int withdrawAge) {
        return account.compare(new AccountSimService.Params(
                lumpSum, annualContribution, years, annualReturn, dividendYield, lowIncome, withdrawAge));
    }
}
