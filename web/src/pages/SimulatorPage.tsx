import { useState, type ReactNode } from 'react'

import { isUnauthorized, login, useMe } from '../lib/auth'
import { formatMoney } from '../lib/dashboard'
import { useDividend, type DividendInput } from '../lib/sim'

const DEFAULTS: DividendInput = {
  principal: 100_000_000,
  yieldPercent: 8,
  otherFinancialIncome: 0,
}

export function SimulatorPage() {
  const me = useMe()
  const [input, setInput] = useState<DividendInput>(DEFAULTS)
  const { data } = useDividend(input)

  if (me.isLoading) return <p className="text-muted">불러오는 중…</p>
  if (isUnauthorized(me.error)) {
    return (
      <section className="space-y-6">
        <h1 className="text-2xl font-bold">로그인이 필요합니다</h1>
        <button
          type="button"
          onClick={login}
          className="rounded-lg bg-accent px-5 py-3 font-semibold text-white hover:bg-accent-hover"
        >
          Keycloak 으로 로그인
        </button>
      </section>
    )
  }

  const set = (k: keyof DividendInput, v: number) => setInput((s) => ({ ...s, [k]: v }))

  return (
    <section className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">시뮬레이터 · ETF 세후 월배당</h1>
        <p className="mt-1 text-sm text-muted">
          배당소득세 15.4%(배당 14% + 지방 1.4%) 분리과세 기준. 연 금융소득 2,000만원 초과 시 종합과세 대상.
        </p>
      </div>

      <div className="grid gap-4 rounded-xl border border-line bg-surface p-5 sm:grid-cols-3">
        <Field label="투자 원금 (원)">
          <NumberInput value={input.principal} step={10_000_000} onChange={(v) => set('principal', v)} />
        </Field>
        <Field label="연 배당수익률 (%)">
          <NumberInput value={input.yieldPercent} step={0.5} onChange={(v) => set('yieldPercent', v)} />
        </Field>
        <Field label="기타 금융소득 (원/년)">
          <NumberInput
            value={input.otherFinancialIncome}
            step={1_000_000}
            onChange={(v) => set('otherFinancialIncome', v)}
          />
        </Field>
      </div>

      {data && (
        <div className="space-y-5">
          <div className="rounded-xl border border-line bg-surface p-6">
            <span className="text-sm text-muted">세후 월평균 배당</span>
            <div className="mt-1 text-4xl font-extrabold text-accent">
              {formatMoney(data.monthlyAfterTax, 'KRW')}
            </div>
            <div className="mt-1 text-sm text-muted">
              세후 실효 수익률 {data.afterTaxYield.toFixed(2)}% · 연 {formatMoney(data.annualAfterTax, 'KRW')}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Card label="세전 연배당" value={formatMoney(data.annualGross, 'KRW')} />
            <Card label={`세금 (${(data.taxRate * 100).toFixed(1)}%)`} value={`-${formatMoney(data.taxAmount, 'KRW')}`} loss />
            <Card label="세후 연배당" value={formatMoney(data.annualAfterTax, 'KRW')} />
          </div>

          {data.comprehensiveTaxable && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
              <p className="font-medium">금융소득종합과세 대상입니다.</p>
              <p className="mt-1 text-amber-700">
                연 금융소득(배당+기타)이 2,000만원을 초과합니다. 초과분은 다른 소득과 합산해 누진세율
                (6~45%)이 적용될 수 있어, 실제 세금은 위 분리과세(15.4%) 기준보다 클 수 있습니다.
              </p>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="text-muted">{label}</span>
      {children}
      <style>{`.siminput{width:100%;border:1px solid var(--color-line);background:#fff;border-radius:8px;padding:8px 10px;font-size:14px;color:var(--color-ink)}.siminput:focus{outline:2px solid var(--color-accent)}`}</style>
    </label>
  )
}

function NumberInput({
  value,
  onChange,
  step = 1,
}: {
  value: number
  onChange: (v: number) => void
  step?: number
}) {
  return (
    <input
      type="number"
      className="siminput"
      value={value}
      step={step}
      min={0}
      onChange={(e) => onChange(Math.max(0, Number(e.target.value)))}
    />
  )
}

function Card({ label, value, loss }: { label: string; value: string; loss?: boolean }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border border-line bg-surface p-5">
      <span className="text-xs text-muted">{label}</span>
      <span className={`text-lg font-bold ${loss ? 'text-loss' : 'text-ink'}`}>{value}</span>
    </div>
  )
}
