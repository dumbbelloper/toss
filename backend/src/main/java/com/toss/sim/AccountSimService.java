package com.toss.sim;

import com.toss.tax.TaxService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * SIM02 — 계좌유형(일반·ISA·연금) 비교. 같은 투자금을 N년 넣었을 때 세후 결과를 나란히.
 * 규칙·세율 단일 출처: docs/product/retirement-tax.md (해외 ETF 가정).
 *
 * <p>가정(문서화): 자산은 해외 ETF(배당 + 가격성장). 배당은 매년 과세(일반만), 가격성장은 인출 시 과세.
 * 일반의 연 배당세는 effective-rate 드래그로 근사. 연금은 분할 연금수령(저율) 가정.
 */
@Service
public class AccountSimService {

    /** lowIncome = 서민형 ISA / 세액공제 16.5% 적용 여부. withdrawAge = 연금 수령 나이. */
    public record Params(double lumpSum, double annualContribution, int years,
                         double annualReturn, double dividendYield, boolean lowIncome, int withdrawAge) {
    }

    public record AccountResult(String account, double contributed, double finalPretax,
                                double finalAfterTax, double tax, double taxBenefit,
                                double totalReturn, List<Double> timeline) {
    }

    public record CompareResult(int years, List<Integer> labels,
                                AccountResult general, AccountResult isa, AccountResult pension) {
    }

    public CompareResult compare(Params p) {
        if (p.years() < 1 || p.years() > 60) {
            throw new IllegalArgumentException("기간은 1~60년이어야 합니다.");
        }
        List<Integer> labels = new ArrayList<>();
        for (int y = 1; y <= p.years(); y++) {
            labels.add(y);
        }
        return new CompareResult(p.years(), labels, general(p), isa(p), pension(p));
    }

    /** 일반계좌: 매년 배당 15.4% 드래그(effective rate), 종료 시 해외 양도세 22%(250만 공제). */
    private AccountResult general(Params p) {
        double effRate = p.annualReturn() - p.dividendYield() * TaxService.DIVIDEND_RATE; // 배당세 드래그
        List<Double> timeline = new ArrayList<>();
        double bal = grow(p, effRate, timeline);
        double contributed = contributed(p);
        double gain = Math.max(0, bal - contributed);
        double tax = Math.max(0, gain - TaxService.FOREIGN_GAIN_DEDUCTION) * TaxService.CAPITAL_GAINS_RATE;
        double after = bal - tax;
        return new AccountResult("general", contributed, bal, after, tax, 0,
                contributed > 0 ? after / contributed - 1 : 0, timeline);
    }

    /** ISA: 보유 비과세(full r), 만기 정산 — 이익 중 비과세 한도 초과분 9.9%. */
    private AccountResult isa(Params p) {
        List<Double> timeline = new ArrayList<>();
        double bal = grow(p, p.annualReturn(), timeline);
        double contributed = contributed(p);
        double gain = Math.max(0, bal - contributed);
        double exempt = p.lowIncome() ? TaxService.ISA_EXEMPT_LOW_INCOME : TaxService.ISA_EXEMPT_GENERAL;
        double tax = Math.max(0, gain - exempt) * TaxService.ISA_EXCESS_RATE;
        double after = bal - tax;
        return new AccountResult("isa", contributed, bal, after, tax, 0,
                contributed > 0 ? after / contributed - 1 : 0, timeline);
    }

    /** 연금(연금저축/IRP): 세액공제 환급(누적) + 과세이연(full r), 인출 시 연금소득세(저율). */
    private AccountResult pension(Params p) {
        double creditRate = p.lowIncome() ? TaxService.PENSION_CREDIT_RATE_HIGH : TaxService.PENSION_CREDIT_RATE_STD;
        List<Double> timeline = new ArrayList<>();
        // 성장은 grow 와 동일하되 세액공제 환급을 별도 누적
        double bal = p.lumpSum();
        double refunds = Math.min(p.lumpSum(), TaxService.PENSION_CONTRIB_CREDIT_LIMIT) * creditRate;
        for (int y = 1; y <= p.years(); y++) {
            bal += p.annualContribution();
            bal *= (1 + p.annualReturn());
            refunds += Math.min(p.annualContribution(), TaxService.PENSION_CONTRIB_CREDIT_LIMIT) * creditRate;
            timeline.add(bal);
        }
        double contributed = contributed(p);
        double withdrawTax = bal * TaxService.pensionWithdrawRate(p.withdrawAge()); // 분할 연금수령 저율 가정
        double after = bal - withdrawTax + refunds; // 세액공제 환급은 현금 이익
        return new AccountResult("pension", contributed, bal, after, withdrawTax, refunds,
                contributed > 0 ? after / contributed - 1 : 0, timeline);
    }

    /** 공통 적립 성장: 연초 적립 후 rate 로 성장. 연도별 잔액 timeline 기록. */
    private double grow(Params p, double rate, List<Double> timeline) {
        double bal = p.lumpSum();
        for (int y = 1; y <= p.years(); y++) {
            bal += p.annualContribution();
            bal *= (1 + rate);
            timeline.add(bal);
        }
        return bal;
    }

    private double contributed(Params p) {
        return p.lumpSum() + p.annualContribution() * p.years();
    }
}
